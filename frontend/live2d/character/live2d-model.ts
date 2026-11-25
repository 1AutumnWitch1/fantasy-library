/**
 * Live2D 角色封装（Cubism SDK for Web 5-r.5）。
 *
 * 这个类只做一件事：把 model3.json 里声明的资源全部加载好，然后每一帧
 *  1. 播放动作 / 待机动作（motion）
 *  2. 把鼠标跟随叠加到眼睛、头部参数上
 *  3. 交给 SDK 的 UpdateScheduler 依次做：眨眼 → 表情 → 呼吸 → 物理 → 姿势
 *  4. 提交参数更新并用 WebGL 渲染
 *
 * 关键点：跟随参数是在 saveParameters() 之后「叠加」进去的，所以不会把
 * 动作的关键帧覆盖掉；而眨眼 / 呼吸 / 物理由官方 effect 类处理，也不用自己
 * 硬编码动画。
 */
import { CubismDefaultParameterId } from '@framework/cubismdefaultparameterid';
import { CubismModelSettingJson } from '@framework/cubismmodelsettingjson';
import { ICubismModelSetting } from '@framework/icubismmodelsetting';
import type { CubismIdHandle } from '@framework/id/cubismid';
import { CubismFramework } from '@framework/live2dcubismframework';
import { CubismMatrix44 } from '@framework/math/cubismmatrix44';
import { CubismModelMatrix } from '@framework/math/cubismmodelmatrix';
import { CubismModel } from '@framework/model/cubismmodel';
import { CubismUserModel } from '@framework/model/cubismusermodel';
import { ACubismMotion } from '@framework/motion/acubismmotion';
import { CubismMotion } from '@framework/motion/cubismmotion';
import { BreathParameterData, CubismBreath } from '@framework/effect/cubismbreath';
import { CubismEyeBlink } from '@framework/effect/cubismeyeblink';
import { CubismUpdateScheduler } from '@framework/motion/cubismupdatescheduler';
import { CubismBreathUpdater } from '@framework/motion/cubismbreathupdater';
import { CubismEyeBlinkUpdater } from '@framework/motion/cubismeyeblinkupdater';
import { CubismExpressionUpdater } from '@framework/motion/cubismexpressionupdater';
import { CubismPhysicsUpdater } from '@framework/motion/cubismphysicsupdater';
import { CubismPoseUpdater } from '@framework/motion/cubismposeupdater';
import { CubismShaderManager_WebGL } from '@framework/rendering/cubismshader_webgl';
import { type DemoModelConfig, SHADER_BASE_PATH } from './model-config';
import { createPremultipliedTexture } from './texture';

/** 动作优先级，数值越大越优先。与官方示例中的定义一致。 */
const PRIORITY_IDLE = 1;
const PRIORITY_FORCE = 3;

/** 鼠标跟随的平滑速度（越大越跟手）。 */
const LOOK_SMOOTHING = 9.0;

/** 模型在画面中的留白比例，避免刚好贴边被裁切。 */
const VIEW_PADDING = 0.92;

export interface MotionGroupInfo {
  /** model3.json 中的组名，例如 Idle / Tap / Flick */
  name: string;
  /** 成功加载的动作下标（不是所有文件都保证加载成功） */
  indices: number[];
}

export interface ModelFeatures {
  eyeBlink: boolean;
  breath: boolean;
  physics: boolean;
  pose: boolean;
  idleMotion: boolean;
  eyeBallFollow: boolean;
  headFollow: boolean;
  expressions: number;
  hitAreas: string[];
}

export interface ModelInfo {
  modelName: string;
  motionGroups: MotionGroupInfo[];
  expressions: string[];
  features: ModelFeatures;
  warnings: string[];
}

export interface InteractionResult {
  type: 'motion' | 'expression';
  label: string;
}

export interface LoadOptions {
  gl: WebGLRenderingContext | WebGL2RenderingContext;
  config: DemoModelConfig;
  canvasWidth: number;
  canvasHeight: number;
  onProgress?: (message: string) => void;
}

export class Live2DCharacter extends CubismUserModel {
  private constructor() {
    super();

    const idManager = CubismFramework.getIdManager();
    this._idAngleX = idManager.getId(CubismDefaultParameterId.ParamAngleX);
    this._idAngleY = idManager.getId(CubismDefaultParameterId.ParamAngleY);
    this._idAngleZ = idManager.getId(CubismDefaultParameterId.ParamAngleZ);
    this._idBodyAngleX = idManager.getId(
      CubismDefaultParameterId.ParamBodyAngleX
    );
    this._idEyeBallX = idManager.getId(
      CubismDefaultParameterId.ParamEyeBallX
    );
    this._idEyeBallY = idManager.getId(
      CubismDefaultParameterId.ParamEyeBallY
    );
    this._idEyeLOpen = idManager.getId(
      CubismDefaultParameterId.ParamEyeLOpen
    );
    this._idEyeROpen = idManager.getId(
      CubismDefaultParameterId.ParamEyeROpen
    );
    this._idBreath = idManager.getId(CubismDefaultParameterId.ParamBreath);
  }

  /**
   * 加载模型及其全部资源。任一步失败都会抛出带中文说明的 Error。
   */
  public static async load(options: LoadOptions): Promise<Live2DCharacter> {
    const character = new Live2DCharacter();
    try {
      await character.setup(options);
      return character;
    } catch (error) {
      character.release();
      throw error;
    }
  }

  private async setup(options: LoadOptions): Promise<void> {
    const { gl, config, canvasWidth, canvasHeight, onProgress } = options;
    const progress = (message: string) => onProgress?.(message);

    this._gl = gl;
    this._config = config;
    this._modelHomeDir = config.dir;

    progress('读取 model3.json…');
    const settingBuffer = await fetchArrayBuffer(config.dir + config.model3);
    const setting = new CubismModelSettingJson(
      settingBuffer,
      settingBuffer.byteLength
    );
    this._modelSetting = setting;

    // ---------- moc3 ----------
    const modelFileName = setting.getModelFileName();
    if (!modelFileName) {
      throw new Error(
        'model3.json 中缺少 FileReferences.Moc，无法加载 Live2D 模型。' +
          '请确认这是从 Cubism Editor 导出的运行时 model3.json。'
      );
    }

    progress(`加载 moc3 模型数据：${modelFileName}`);
    const mocBuffer = await fetchArrayBuffer(this._modelHomeDir + modelFileName);
    this.loadModel(mocBuffer);

    if (!this.getModel()) {
      throw new Error(
        'moc3 加载失败。可能原因：moc3 文件损坏，或由比 SDK 5-r.5 更新的 Cubism Editor 导出。' +
          '请使用与当前 SDK 匹配的版本重新导出。'
      );
    }

    this._hasEyeBall =
      this.hasParameter(this._idEyeBallX) &&
      this.hasParameter(this._idEyeBallY);
    this._hasHeadAngle =
      this.hasParameter(this._idAngleX) &&
      this.hasParameter(this._idAngleY);
    this._hasAngleZ = this.hasParameter(this._idAngleZ);
    this._hasBodyAngle = this.hasParameter(this._idBodyAngleX);

    // ---------- 眨眼 ----------
    if (setting.getEyeBlinkParameterCount() > 0) {
      this._eyeBlink = CubismEyeBlink.create(setting);
    }
    this._eyeBlinkEnabled = this._eyeBlink != null;
    if (this._eyeBlink) {
      this._updateScheduler.addUpdatableList(
        new CubismEyeBlinkUpdater(() => this._motionUpdated, this._eyeBlink)
      );
    } else {
      this._warnings.push(
        '模型未定义 EyeBlink 参数组，自动眨眼不可用（不会伪造）。'
      );
    }

    // ---------- 呼吸 ----------
    const breathParameters: BreathParameterData[] = [];
    if (this._hasHeadAngle) {
      breathParameters.push(
        new BreathParameterData(this._idAngleX, 0.0, 15.0, 6.5345, 0.5),
        new BreathParameterData(this._idAngleY, 0.0, 8.0, 3.5345, 0.5)
      );
    }
    if (this._hasAngleZ) {
      breathParameters.push(
        new BreathParameterData(this._idAngleZ, 0.0, 10.0, 5.5345, 0.5)
      );
    }
    if (this._hasBodyAngle) {
      breathParameters.push(
        new BreathParameterData(this._idBodyAngleX, 0.0, 4.0, 15.5345, 0.5)
      );
    }
    if (this.hasParameter(this._idBreath)) {
      breathParameters.push(
        new BreathParameterData(this._idBreath, 0.5, 0.5, 3.2345, 1.0)
      );
    }

    if (breathParameters.length > 0) {
      this._breath = CubismBreath.create();
      this._breath.setParameters(breathParameters);
      this._breathEnabled = true;
      this._updateScheduler.addUpdatableList(
        new CubismBreathUpdater(this._breath)
      );
    } else {
      this._warnings.push('模型没有呼吸相关参数，呼吸动画不可用。');
    }

    // ---------- 物理演算（头发 / 衣服晃动） ----------
    const physicsFileName = setting.getPhysicsFileName();
    if (physicsFileName) {
      progress(`加载物理演算：${physicsFileName}`);
      try {
        const physicsBuffer = await fetchArrayBuffer(
          this._modelHomeDir + physicsFileName
        );
        this.loadPhysics(physicsBuffer, physicsBuffer.byteLength);
        if (this._physics) {
          this._updateScheduler.addUpdatableList(
            new CubismPhysicsUpdater(this._physics)
          );
        }
      } catch (error) {
        this._warnings.push(
          `物理演算加载失败，头发/衣服将不会晃动：${errorMessage(error)}`
        );
      }
    } else {
      this._warnings.push('model3.json 未声明 Physics，模型没有物理演算。');
    }

    // ---------- Pose（部件显隐切换） ----------
    const poseFileName = setting.getPoseFileName();
    if (poseFileName) {
      progress(`加载 Pose：${poseFileName}`);
      try {
        const poseBuffer = await fetchArrayBuffer(
          this._modelHomeDir + poseFileName
        );
        this.loadPose(poseBuffer, poseBuffer.byteLength);
        if (this._pose) {
          this._updateScheduler.addUpdatableList(
            new CubismPoseUpdater(this._pose)
          );
        }
      } catch (error) {
        this._warnings.push(`Pose 加载失败：${errorMessage(error)}`);
      }
    }

    // ---------- 眨眼 / 口型参数 ID（给动作使用） ----------
    this._eyeBlinkIds = [];
    for (let i = 0; i < setting.getEyeBlinkParameterCount(); i++) {
      this._eyeBlinkIds.push(setting.getEyeBlinkParameterId(i));
    }
    this._lipSyncIds = [];
    for (let i = 0; i < setting.getLipSyncParameterCount(); i++) {
      this._lipSyncIds.push(setting.getLipSyncParameterId(i));
    }

    // ---------- 动作 ----------
    await this.loadMotions(progress);

    // ---------- 表情（Hiyori 没有 exp3.json，会自动跳过） ----------
    await this.loadExpressions(progress);

    // 表情更新器总是注册；没有表情时它会自然什么都不做。
    this._updateScheduler.addUpdatableList(
      new CubismExpressionUpdater(this._expressionManager)
    );

    // ---------- 渲染器 ----------
    progress('创建 WebGL 渲染器…');
    this.createRenderer(canvasWidth, canvasHeight);
    const renderer = this.getRenderer();
    if (!renderer) {
      throw new Error('创建 Cubism 渲染器失败。');
    }

    renderer.startUp(gl);
    renderer.setIsPremultipliedAlpha(true);
    renderer.loadShaders(this._config.shaderPath ?? SHADER_BASE_PATH);

    // ---------- 纹理 ----------
    const textureCount = setting.getTextureCount();
    if (textureCount === 0) {
      throw new Error('model3.json 中没有 Textures，模型无法显示。');
    }

    for (let i = 0; i < textureCount; i++) {
      const textureFileName = setting.getTextureFileName(i);
      if (!textureFileName) continue;

      progress(`加载纹理 ${i + 1}/${textureCount}：${textureFileName}`);
      const texture = await createPremultipliedTexture(
        gl,
        this._modelHomeDir + textureFileName
      );
      renderer.bindTexture(i, texture);
      this._textures.push(texture);
    }

    // SDK 的 loadShaders() 是异步的且没有返回 Promise，
    // 这里等一下它完成，避免第一帧用空着色器绘制。
    progress('等待着色器编译…');
    await waitForShaders(gl);

    // 初始化视口与 MVP 矩阵
    this.updateViewport(canvasWidth, canvasHeight);

    this.getModel().saveParameters();
    this._ready = true;

    progress('进入待机动作…');
    this.startIdleMotion();
  }

  private async loadMotions(
    progress: (message: string) => void
  ): Promise<void> {
    const setting = this._modelSetting;
    const groupCount = setting.getMotionGroupCount();

    for (let groupIndex = 0; groupIndex < groupCount; groupIndex++) {
      const groupName = setting.getMotionGroupName(groupIndex);
      const motionCount = setting.getMotionCount(groupName);
      const indices: number[] = [];

      for (let i = 0; i < motionCount; i++) {
        const motionFileName = setting.getMotionFileName(groupName, i);
        progress(`加载动作 ${groupName} ${i + 1}/${motionCount}…`);

        try {
          const buffer = await fetchArrayBuffer(
            this._modelHomeDir + motionFileName
          );
          const motion = this.loadMotion(
            buffer,
            buffer.byteLength,
            `${groupName}_${i}`,
            null,
            null,
            setting,
            groupName,
            i
          );

          if (!motion) {
            this._warnings.push(
              `动作解析失败：${groupName}[${i}] ${motionFileName}`
            );
            continue;
          }

          // 让动作里的 EyeBlink / LipSync 曲线由 effect 统一管理
          motion.setEffectIds(this._eyeBlinkIds, this._lipSyncIds);
          this._motions.set(`${groupName}_${i}`, motion);
          indices.push(i);
        } catch (error) {
          this._warnings.push(
            `动作加载失败：${groupName}[${i}] ${motionFileName}（${errorMessage(error)}）`
          );
        }
      }

      if (indices.length > 0) {
        this._motionGroups.push({ name: groupName, indices });
      }
    }

    if (this._motionGroups.length === 0) {
      this._warnings.push('模型没有任何可用动作，点击互动与待机动作都不可用。');
    }
  }

  private async loadExpressions(
    progress: (message: string) => void
  ): Promise<void> {
    const setting = this._modelSetting;
    const expressionCount = setting.getExpressionCount();

    for (let i = 0; i < expressionCount; i++) {
      const name = setting.getExpressionName(i);
      const fileName = setting.getExpressionFileName(i);
      progress(`加载表情 ${i + 1}/${expressionCount}：${name}`);

      try {
        const buffer = await fetchArrayBuffer(this._modelHomeDir + fileName);
        const expression = this.loadExpression(buffer, buffer.byteLength, name);
        if (expression) {
          this._expressions.set(name, expression);
          this._expressionNames.push(name);
        }
      } catch (error) {
        this._warnings.push(
          `表情加载失败：${name}（${errorMessage(error)}）`
        );
      }
    }

    if (expressionCount === 0) {
      this._warnings.push(
        '该模型未提供表情（exp3.json），表情切换已禁用（不会调用不存在的表情）。'
      );
    }
  }

  // ------------------------------------------------------------------
  // 每帧更新
  // ------------------------------------------------------------------

  public update(deltaTimeSeconds: number): void {
    if (!this._ready) return;

    const model = this.getModel();
    if (!model) return;

    const deltaTime = Math.min(Math.max(deltaTimeSeconds, 0.0), 0.1);
    this._userTimeSeconds += deltaTime;

    // 1) 还原上一帧保存的参数，再让动作覆盖它
    model.loadParameters();
    this._motionUpdated = false;

    if (this._motionManager.isFinished()) {
      // 没有动作在播放时，随机播放一个 Idle 待机动作
      this.startIdleMotion();
    } else {
      this._motionUpdated = this._motionManager.updateMotion(model, deltaTime);
    }

    // 2) 保存动作结果，之后的跟随 / 特效都叠加在它上面
    model.saveParameters();

    // 3) 鼠标跟随（平滑后叠加，不覆盖动作）
    this.applyLook(deltaTime);

    // 4) 眨眼 → 表情 → 呼吸 → 物理 → Pose（由 SDK 调度器排序执行）
    this._updateScheduler.onLateUpdate(model, deltaTime);

    // 5) 提交参数到模型
    model.update();
  }

  private applyLook(deltaTimeSeconds: number): void {
    const model = this.getModel();
    if (!model) return;

    // 指数平滑：与帧率无关，鼠标移动时跟随自然，离开后缓慢回正
    const smoothing = 1.0 - Math.exp(-deltaTimeSeconds * LOOK_SMOOTHING);
    this._lookX += (this._lookTargetX - this._lookX) * smoothing;
    this._lookY += (this._lookTargetY - this._lookY) * smoothing;

    if (this._eyeFollowEnabled && this._hasEyeBall) {
      // 眼睛使用「带权重的混合」而不是纯叠加：
      // Hiyori 的待机动作本身也会动眼球，纯叠加会被动作抵消。
      // 鼠标靠近中心时权重低（保留待机动作的眼睛小动作），
      // 鼠标偏离中心时权重升到 1（眼睛可靠地看向鼠标）。
      const eyeWeight = clamp(
        Math.max(Math.abs(this._lookX), Math.abs(this._lookY)) * 1.5,
        0.0,
        1.0
      );
      const currentEyeX = model.getParameterValueById(this._idEyeBallX);
      const currentEyeY = model.getParameterValueById(this._idEyeBallY);

      model.setParameterValueById(
        this._idEyeBallX,
        currentEyeX + (this._lookX - currentEyeX) * eyeWeight
      );
      model.setParameterValueById(
        this._idEyeBallY,
        currentEyeY + (this._lookY - currentEyeY) * eyeWeight
      );
    }

    if (this._headFollowEnabled && this._hasHeadAngle) {
      // 头部轻微跟随（±30°，与官方 CubismLook 的系数一致）
      model.addParameterValueById(this._idAngleX, this._lookX * 30.0);
      model.addParameterValueById(this._idAngleY, this._lookY * 30.0);

      if (this._hasAngleZ) {
        // 视线斜向移动时带一点点歪头
        model.addParameterValueById(
          this._idAngleZ,
          this._lookX * this._lookY * -30.0
        );
      }

      if (this._hasBodyAngle) {
        model.addParameterValueById(this._idBodyAngleX, this._lookX * 10.0);
      }
    }
  }

  // ------------------------------------------------------------------
  // 视口 / 绘制 / 命中检测
  // ------------------------------------------------------------------

  /**
   * 根据画布尺寸重新计算「模型铺满画面且不被裁切」的变换。
   * 由 Demo 在窗口 resize 或缩放比例变化时调用。
   */
  public updateViewport(canvasWidth: number, canvasHeight: number): void {
    const model = this.getModel();
    if (!model || canvasWidth <= 0 || canvasHeight <= 0) return;

    const aspect = canvasWidth / canvasHeight;
    const modelWidth = model.getCanvasWidth();
    const modelHeight = model.getCanvasHeight();

    // 画面逻辑坐标：y ∈ [-1, 1]，x ∈ [-aspect, aspect]。
    // 让模型完整放进视口：高度占满，同时宽度也不能超出。
    const fitHeight =
      2.0 *
      VIEW_PADDING *
      this._zoom *
      Math.min(1.0, (aspect * modelHeight) / modelWidth);

    const modelMatrix = new CubismModelMatrix(modelWidth, modelHeight);
    // Cubism 的模型顶点以画布中心为原点（x、y 都有正负），
    // 所以这里只需要设置缩放，不需要再做居中平移。
    modelMatrix.setHeight(fitHeight);
    // isHit() 内部用 _modelMatrix 做逆变换，这里保持同步
    this._modelMatrix = modelMatrix;

    const projection = new CubismMatrix44();
    projection.scale(1.0 / aspect, 1.0);

    const mvp = new CubismMatrix44();
    // 注意：SDK 的 CubismMatrix44.multiply(a, b, dst) 计算的是 b * a，
    // 因此这里把 modelMatrix 放在第一个参数、projection 放在第二个参数，
    // 得到的就是 clip = projection * modelMatrix * vertex。
    CubismMatrix44.multiply(
      modelMatrix.getArray(),
      projection.getArray(),
      mvp.getArray()
    );

    this._mvp = mvp;
    this._viewport = [0, 0, canvasWidth, canvasHeight];
  }

  public draw(): void {
    const renderer = this.getRenderer();
    if (!this._ready || !renderer || !this._mvp) return;

    renderer.setMvpMatrix(this._mvp);
    renderer.setRenderState(null, this._viewport);
    renderer.drawModel(this._config.shaderPath ?? SHADER_BASE_PATH);
  }

  /**
   * 命中检测。坐标使用画面逻辑坐标：x ∈ [-aspect, aspect]，y ∈ [-1, 1]。
   * 返回命中的 HitArea 名称，未命中返回 null。
   */
  public hitTest(logicalX: number, logicalY: number): string | null {
    if (!this._ready || !this._modelSetting) return null;

    const count = this._modelSetting.getHitAreasCount();
    for (let i = 0; i < count; i++) {
      if (this.isHit(this._modelSetting.getHitAreaId(i), logicalX, logicalY)) {
        return this._modelSetting.getHitAreaName(i);
      }
    }
    return null;
  }

  // ------------------------------------------------------------------
  // 对外控制
  // ------------------------------------------------------------------

  public setLookTarget(x: number, y: number): void {
    this._lookTargetX = clamp(x, -1.0, 1.0);
    this._lookTargetY = clamp(y, -1.0, 1.0);
  }

  /** 鼠标离开窗口时让角色慢慢回到默认姿态。 */
  public resetLookTarget(): void {
    this._lookTargetX = 0.0;
    this._lookTargetY = 0.0;
  }

  public setEyeFollowEnabled(enabled: boolean): void {
    this._eyeFollowEnabled = enabled;
  }

  public setHeadFollowEnabled(enabled: boolean): void {
    this._headFollowEnabled = enabled;
  }

  public setZoom(zoom: number): void {
    this._zoom = clamp(zoom, 0.5, 2.0);
  }

  public getZoom(): number {
    return this._zoom;
  }

  /**
   * 播放指定组的指定动作。index 是 model3.json 中该组的下标。
   * 用户主动播放 / 点击互动时使用最高优先级，会打断当前动作（旧动作淡出）。
   */
  public startMotion(
    group: string,
    index: number,
    priority: number = PRIORITY_FORCE
  ): boolean {
    const motion = this._motions.get(`${group}_${index}`);
    if (!motion) {
      return false;
    }

    if (priority === PRIORITY_FORCE) {
      this._motionManager.setReservePriority(priority);
    }
    this._motionManager.startMotionPriority(motion, false, priority);
    return true;
  }

  public startRandomMotion(
    group: string,
    priority: number = PRIORITY_FORCE
  ): string | null {
    const groupInfo = this._motionGroups.find(
      item => item.name === group && item.indices.length > 0
    );
    if (!groupInfo) return null;

    const index =
      groupInfo.indices[Math.floor(Math.random() * groupInfo.indices.length)];
    return this.startMotion(group, index, priority) ? `${group}_${index}` : null;
  }

  private startIdleMotion(): boolean {
    const idle = this._motionGroups.find(
      item => item.name === 'Idle' && item.indices.length > 0
    );
    if (!idle) return false;

    const index = idle.indices[Math.floor(Math.random() * idle.indices.length)];
    return this.startMotion('Idle', index, PRIORITY_IDLE);
  }

  public setExpression(name: string): boolean {
    const expression = this._expressions.get(name);
    if (!expression) return false;

    this._expressionManager.startMotion(expression, false);
    return true;
  }

  /**
   * 点击互动：模型有表情时随机切换表情，否则随机播放一个非 Idle 的动作。
   * 只会选择「实际加载成功」的表情 / 动作。
   */
  public playRandomInteraction(): InteractionResult | null {
    const interactionGroups = this._motionGroups.filter(
      item => item.name !== 'Idle' && item.indices.length > 0
    );
    const canUseExpression = this._expressionNames.length > 0;
    const canUseMotion = interactionGroups.length > 0;

    if (!canUseExpression && !canUseMotion) {
      return null;
    }

    // 有表情时，40% 概率切换表情；否则播放动作
    if (canUseExpression && (!canUseMotion || Math.random() < 0.4)) {
      const name =
        this._expressionNames[
          Math.floor(Math.random() * this._expressionNames.length)
        ];
      if (this.setExpression(name)) {
        return { type: 'expression', label: name };
      }
    }

    if (canUseMotion) {
      const group =
        interactionGroups[Math.floor(Math.random() * interactionGroups.length)];
      const index =
        group.indices[Math.floor(Math.random() * group.indices.length)];

      if (this.startMotion(group.name, index, PRIORITY_FORCE)) {
        return { type: 'motion', label: `${group.name}_${index}` };
      }
    }

    // 动作播放失败时再用表情兜底
    if (canUseExpression) {
      const name = this._expressionNames[0];
      if (this.setExpression(name)) {
        return { type: 'expression', label: name };
      }
    }

    return null;
  }

  /**
   * 重置角色姿态：停掉动作与表情，把所有参数恢复为模型默认值，
   * 清空鼠标跟随的累积角度，然后重新进入待机动作。
   */
  public resetPose(): void {
    const model = this.getModel();
    if (!model) return;

    this._motionManager.stopAllMotions();
    this._expressionManager.stopAllMotions();
    this._motionManager.setReservePriority(0);
    this._motionUpdated = false;

    this._lookX = 0.0;
    this._lookY = 0.0;
    this._lookTargetX = 0.0;
    this._lookTargetY = 0.0;

    for (let i = 0; i < model.getParameterCount(); i++) {
      model.setParameterValueByIndex(i, model.getParameterDefaultValue(i));
    }
    model.saveParameters();

    if (this._pose) {
      this._pose.reset(model);
    }

    this.startIdleMotion();
  }

  public getInfo(): ModelInfo {
    const setting = this._modelSetting;
    const hitAreas: string[] = [];

    if (setting) {
      for (let i = 0; i < setting.getHitAreasCount(); i++) {
        hitAreas.push(setting.getHitAreaName(i));
      }
    }

    return {
      modelName: this._config.model3,
      motionGroups: this._motionGroups.map(group => ({
        name: group.name,
        indices: [...group.indices]
      })),
      expressions: [...this._expressionNames],
      features: {
        eyeBlink: this._eyeBlinkEnabled,
        breath: this._breathEnabled,
        physics: this._physics != null,
        pose: this._pose != null,
        idleMotion: this._motionGroups.some(
          group => group.name === 'Idle' && group.indices.length > 0
        ),
        eyeBallFollow: this._hasEyeBall,
        headFollow: this._hasHeadAngle,
        expressions: this._expressionNames.length,
        hitAreas
      },
      warnings: [...this._warnings]
    };
  }

  public getMotionCount(): number {
    let count = 0;
    for (const group of this._motionGroups) {
      count += group.indices.length;
    }
    return count;
  }

  /**
   * 读取几个关键参数的当前值，方便在控制台检查动画是否真的在跑。
   * （不参与渲染，只用于调试 / 自动化验证）
   */
  public getDebugState(): Record<string, number> {
    const model = this.getModel();
    if (!model) return {};

    const read = (parameterId: CubismIdHandle): number => {
      const index = model.getParameterIndex(parameterId);
      return index >= 0 ? model.getParameterValueByIndex(index) : Number.NaN;
    };

    return {
      angleX: read(this._idAngleX),
      angleY: read(this._idAngleY),
      angleZ: read(this._idAngleZ),
      bodyAngleX: read(this._idBodyAngleX),
      eyeBallX: read(this._idEyeBallX),
      eyeBallY: read(this._idEyeBallY),
      eyeLOpen: read(this._idEyeLOpen),
      eyeROpen: read(this._idEyeROpen),
      breath: read(this._idBreath),
      // 跟随输入与开关状态（0/1），方便确认跟随逻辑是否在生效
      lookX: this._lookX,
      lookY: this._lookY,
      eyeFollowEnabled: this._eyeFollowEnabled ? 1 : 0,
      headFollowEnabled: this._headFollowEnabled ? 1 : 0
    };
  }

  private hasParameter(parameterId: CubismIdHandle): boolean {
    const model: CubismModel | null = this.getModel();
    return model != null && model.getParameterIndex(parameterId) >= 0;
  }

  public release(): void {
    this._ready = false;

    this._motions.clear();
    this._expressions.clear();

    if (this._gl) {
      for (const texture of this._textures) {
        this._gl.deleteTexture(texture);
      }
    }
    this._textures.length = 0;

    this._updateScheduler.release();

    super.release();
  }

  private _gl: WebGLRenderingContext | WebGL2RenderingContext;
  private _config: DemoModelConfig;
  private _modelSetting: ICubismModelSetting;
  private _modelHomeDir: string;
  private _userTimeSeconds = 0.0;
  private _ready = false;

  private _motionGroups: MotionGroupInfo[] = [];
  private _motions = new Map<string, CubismMotion>();
  private _expressions = new Map<string, ACubismMotion>();
  private _expressionNames: string[] = [];
  private _warnings: string[] = [];

  private _updateScheduler = new CubismUpdateScheduler();
  private _motionUpdated = false;

  private _textures: WebGLTexture[] = [];
  private _mvp: CubismMatrix44 | null = null;
  private _viewport: number[] = [0, 0, 1, 1];
  private _zoom = 1.0;

  private _lookTargetX = 0.0;
  private _lookTargetY = 0.0;
  private _lookX = 0.0;
  private _lookY = 0.0;

  private _eyeFollowEnabled = true;
  private _headFollowEnabled = true;

  private _eyeBlinkEnabled = false;
  private _breathEnabled = false;
  private _hasEyeBall = false;
  private _hasHeadAngle = false;
  private _hasAngleZ = false;
  private _hasBodyAngle = false;

  private _idAngleX: CubismIdHandle;
  private _idAngleY: CubismIdHandle;
  private _idAngleZ: CubismIdHandle;
  private _idBodyAngleX: CubismIdHandle;
  private _idEyeBallX: CubismIdHandle;
  private _idEyeBallY: CubismIdHandle;
  private _idEyeLOpen: CubismIdHandle;
  private _idEyeROpen: CubismIdHandle;
  private _idBreath: CubismIdHandle;

  private _eyeBlinkIds: CubismIdHandle[] = [];
  private _lipSyncIds: CubismIdHandle[] = [];
}

// ----------------------------------------------------------------------
// 工具函数
// ----------------------------------------------------------------------

async function fetchArrayBuffer(url: string): Promise<ArrayBuffer> {
  let response: Response;

  try {
    response = await fetch(url);
  } catch {
    throw new Error(
      `无法请求：${url}\n` +
        '请确认静态资源可访问：开发时运行 npm run dev，构建产物用 npm run preview 预览；' +
        '并确认文件已由 scripts/prepare-assets.mjs 复制到 public/。'
    );
  }

  if (!response.ok) {
    throw new Error(
      `请求失败（HTTP ${response.status}）：${url}\n` +
        '请检查 scripts/prepare-assets.mjs 是否复制成功，或模型目录是否完整。'
    );
  }

  return await response.arrayBuffer();
}

/**
 * Cubism SDK 的 loadShaders() 内部是 async 但没有返回 Promise，
 * 这里轮询它公开的标志位，确保第一帧绘制时着色器已就绪。
 */
async function waitForShaders(
  gl: WebGLRenderingContext | WebGL2RenderingContext
): Promise<void> {
  const manager = CubismShaderManager_WebGL.getInstance();
  const glContext = gl as unknown as WebGLRenderingContext;
  const deadline = performance.now() + 8000;

  while (performance.now() < deadline) {
    const shader = manager.getShader(glContext);
    if (shader && shader._isShaderLoaded) {
      return;
    }
    await delay(25);
  }

  throw new Error(
    '着色器加载超时。请确认 public/Framework/Shaders/WebGL/ 下存在 13 个 .vert/.frag 文件，' +
      '并查看浏览器 Network 面板是否有 404。'
  );
}

function delay(milliseconds: number): Promise<void> {
  return new Promise(resolve => setTimeout(resolve, milliseconds));
}

function clamp(value: number, min: number, max: number): number {
  return Math.min(Math.max(value, min), max);
}

function errorMessage(error: unknown): string {
  return error instanceof Error ? error.message : String(error);
}
