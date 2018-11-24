package services

import java.io.File

import scala.util.Try

import com.google.inject.{Inject, Singleton}
import daos.AttachmentDao
import play.api.Configuration
import scala.language.postfixOps
import scala.sys.process._


@Singleton
class AttachmentService @Inject()(
  attachmentDao: AttachmentDao,
  config: Configuration
) {

  def saveFile(from: String, path: String, filename: String, userId: Int, size: Long) = {
    attachmentDao.saveFile(from, path, filename, userId, size)
  }

  def find(uuid: String, groupIdO: Option[Int]) = {
    for {
      groupId <- groupIdO
      path <- config.getOptional[String]("path.upload")
      attach <- attachmentDao.find(uuid)
      filePath = s"$path/$groupId/$uuid"
      output <- Try(Seq("sh", "-c", s"ls $filePath -1") !!).toOption
      fileName <- output.split("\\n").headOption if fileName == attach.filename
      file <- Try(new File(s"$filePath/$fileName")).toOption
    } yield file
  }

  def findByUserId(userId: Int) = {
    attachmentDao.findByUserId(userId)
  }
}
