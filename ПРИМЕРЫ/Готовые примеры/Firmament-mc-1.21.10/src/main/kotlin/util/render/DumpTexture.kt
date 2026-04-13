package moe.nea.firmament.util.render

import com.mojang.blaze3d.buffers.GpuBuffer
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.textures.GpuTexture
import java.io.File
import com.mojang.blaze3d.platform.NativeImage

fun dumpTexture(gpuTexture: GpuTexture, name: String) {
	val w = gpuTexture.getWidth(0)
	val h = gpuTexture.getHeight(0)
	val buffer = RenderSystem.getDevice()
		.createBuffer(
			{ "Dump Buffer" },
			GpuBuffer.USAGE_COPY_DST or GpuBuffer.USAGE_MAP_READ,
			w * h * gpuTexture.getFormat().pixelSize()
		)
	val commandEncoder = RenderSystem.getDevice().createCommandEncoder()
	commandEncoder.copyTextureToBuffer(
		gpuTexture, buffer, 0, {
			val nativeImage = NativeImage(w, h, false)
			commandEncoder.mapBuffer(buffer, true, false).use { mappedView ->
				for (i in 0..<w) {
					for (j in 0..<h) {
						val color = mappedView.data().getInt((j + i * w) * gpuTexture.format.pixelSize())
						nativeImage.setPixelABGR(j, h - i - 1, color)
					}
				}
			}
			buffer.close()
			nativeImage.writeToFile(File("$name.png"))
		}, 0
	)
}
