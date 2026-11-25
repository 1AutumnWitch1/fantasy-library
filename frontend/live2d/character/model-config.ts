/**
 * Demo 的模型配置。
 *
 * 想换模型时，只需要：
 *   1. 把新模型的 runtime 目录（含 .moc3 / .model3.json / 纹理 / 动作 / 物理）复制到
 *      live2d-demo/public/models/<id>/ 下；
 *   2. 在下面 MODELS 里加一项，并把 ACTIVE_MODEL_ID 改成新模型的 id；
 *   3. 重新运行 npm run dev。
 *
 * 注意：不要修改原模型文件，复制一份到 public/models/ 即可。
 */
export interface DemoModelConfig {
  /** 模型 id，同时作为 public/models 下的目录名 */
  id: string;
  /** 面板上显示的名字 */
  label: string;
  /** public 下的模型目录，必须以 / 开头并以 / 结尾 */
  dir: string;
  /** .model3.json 的文件名（位于 dir 内） */
  model3: string;
  shaderPath?: string;
}

export const MODELS: Record<string, DemoModelConfig> = {
  hiyori_pro: {
    id: 'hiyori_pro',
    label: '桃濑日和 PRO（hiyori_pro_t11）',
    dir: '/models/hiyori_pro/',
    model3: 'hiyori_pro_t11.model3.json'
  },
  hiyori_free: {
    id: 'hiyori_free',
    label: '桃濑日和 FREE（hiyori_free_t08）',
    dir: '/models/hiyori_free/',
    model3: 'hiyori_free_t08.model3.json'
  }
};

/** 当前使用的模型。想切换成免费版就改成 'hiyori_free'。 */
export const ACTIVE_MODEL_ID = 'hiyori_pro';

export const ACTIVE_MODEL: DemoModelConfig = MODELS[ACTIVE_MODEL_ID];

/**
 * Cubism Framework 的 WebGL 着色器目录（由 scripts/prepare-assets.mjs 复制到 public 下）。
 * 使用绝对路径，dev / preview 都能访问。
 */
export const SHADER_BASE_PATH = '/Framework/Shaders/WebGL/';
