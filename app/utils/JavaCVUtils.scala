package utils

import java.io.{BufferedOutputStream, ByteArrayInputStream, ByteArrayOutputStream, File, PipedInputStream, PipedOutputStream}

import javax.imageio.ImageIO
import org.bytedeco.javacv.{FFmpegFrameGrabber, FFmpegFrameRecorder, FFmpegLogCallback, Java2DFrameUtils}
import org.bytedeco.ffmpeg.global.avcodec._
import org.bytedeco.ffmpeg.global.avutil._

object JavaCVUtils {

  def createVideoPreview(file: File) = {

    // debug stuff
    FFmpegLogCallback.set()
    av_log_set_level(AV_LOG_DEBUG)
    var frameGrabber = new FFmpegFrameGrabber(file)
    frameGrabber.start()
    var frame = frameGrabber.grabImage()
    //        CanvasFrame canvasFrame = new CanvasFrame("LOL");
    //        canvasFrame.setCanvasSize(frame.imageWidth, frame.imageHeight);
    val FRAME_COUNT_ORIG = frameGrabber.getFrameRate
    val FRAME_COUNT = (FRAME_COUNT_ORIG / 2).toInt

    var aspectRatio: Double = frame.imageWidth / frame.imageHeight.toDouble
    val height:Int = if (frame.imageWidth > 480) 480 else frame.imageWidth
    val width:Int = (height * aspectRatio).toInt

    val bufOutputVideo = new ByteArrayOutputStream()
    val bufOutputImage = new ByteArrayOutputStream()
    val frameRecorder = new FFmpegFrameRecorder(bufOutputVideo, width, height, 0)
    frameRecorder.setFrameRate(FRAME_COUNT)
    frameRecorder.setVideoCodec(AV_CODEC_ID_VP8)
    frameRecorder.setFormat("webm")
    frameRecorder.setAudioChannels(0)
    frameRecorder.start()

    var i: Int = 0
    for (stage <- 1 to 3) {
      i = 0
      var part = frameGrabber.getLengthInVideoFrames / 100
      var startT: Int = part * 15
      var midT: Int = part * 50
      var lastT: Int = part * 85
      stage match {
        case 1 =>
          frameGrabber.setFrameNumber(startT)
          frame = frameGrabber.grabImage()
          //          var outputfile = new File("image.jpg")
          ImageIO.write(Java2DFrameUtils.toBufferedImage(frame), "jpg", bufOutputImage) // output file maybe a stream
        case 2 => frameGrabber.setFrameNumber(midT)
        case 3 => frameGrabber.setFrameNumber(lastT)

      }

      frame = frameGrabber.grabImage()
      while (frame != null && i < FRAME_COUNT_ORIG) {
        frameRecorder.record(frame)
        i += 1
        frame = frameGrabber.grabImage()
      }
    }
    frameRecorder.stop()
    frameRecorder.close()
    bufOutputImage.close()
    bufOutputVideo.close()

    val dataImage:Array[Byte] =  bufOutputImage.toByteArray
    val dataVideo:Array[Byte] =  bufOutputVideo.toByteArray
    (new ByteArrayInputStream(dataImage),new ByteArrayInputStream(dataVideo))
  }
}
