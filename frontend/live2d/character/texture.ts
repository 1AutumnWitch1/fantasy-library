/**
 * 纹理加载：把 PNG 上传为 WebGL 纹理。
 *
 * Cubism 官方示例使用「预乘 Alpha」（premultiplied alpha）以获得更好的
 * 半透明边缘质量，因此这里同步开启 UNPACK_PREMULTIPLY_ALPHA_WEBGL，
 * 并在初始化渲染器时调用 setIsPremultipliedAlpha(true)。
 */

export function loadImage(url: string): Promise<HTMLImageElement> {
  return new Promise((resolve, reject) => {
    const image = new Image();
    image.decoding = 'async';
    image.onload = () => resolve(image);
    image.onerror = () =>
      reject(new Error(`纹理加载失败（文件缺失或无法解码）：${url}`));
    image.src = url;
  });
}

export async function createPremultipliedTexture(
  gl: WebGLRenderingContext | WebGL2RenderingContext,
  url: string
): Promise<WebGLTexture> {
  const image = await loadImage(url);
  const texture = gl.createTexture();

  if (!texture) {
    throw new Error(`无法创建 WebGL 纹理：${url}`);
  }

  gl.bindTexture(gl.TEXTURE_2D, texture);
  gl.pixelStorei(gl.UNPACK_PREMULTIPLY_ALPHA_WEBGL, 1);
  gl.pixelStorei(gl.UNPACK_FLIP_Y_WEBGL, 0);

  gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MIN_FILTER, gl.LINEAR_MIPMAP_LINEAR);
  gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MAG_FILTER, gl.LINEAR);
  gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_S, gl.CLAMP_TO_EDGE);
  gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_T, gl.CLAMP_TO_EDGE);

  gl.texImage2D(
    gl.TEXTURE_2D,
    0,
    gl.RGBA,
    gl.RGBA,
    gl.UNSIGNED_BYTE,
    image
  );
  gl.generateMipmap(gl.TEXTURE_2D);

  gl.bindTexture(gl.TEXTURE_2D, null);
  gl.pixelStorei(gl.UNPACK_PREMULTIPLY_ALPHA_WEBGL, 0);

  return texture;
}
