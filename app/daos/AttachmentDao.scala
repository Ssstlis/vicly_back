package daos

import scala.language.postfixOps
import scala.sys.process._
import scala.util.Try

import com.google.inject.{Inject, Singleton}
import com.mongodb.casbah.commons.MongoDBObject
import models.Attachment
import org.bson.types.ObjectId
import ru.tochkak.plugin.salat.PlaySalat
import salat.dao.{ModelCompanion, SalatDAO}

@Singleton
class AttachmentDao @Inject()(
  mongoContext: MongoContext,
  playSalat: PlaySalat
) extends ModelCompanion[Attachment, ObjectId] {

  import mongoContext._

  val dao = new SalatDAO[Attachment, ObjectId](playSalat.collection("attachment", "ms")){}

  private def sh(cmd: String) = {
    Seq("sh", "-c") :+ cmd
  }

  private def mkdir(path: String) = {
    sh(s"mkdir -p $path")
  }

  private def cp(from: String, to: String) = {
    sh(s"cp $from $to")
  }

  private def filename(path: String) = {
    sh(s"ls $path -1") !!
  }

  private def rm(path: String) = {
    sh(s"sudo rm -rf $path") !
  }

  def saveFile(from: String, path: String, filename: String, userId: Int, size: Long) = {
    val result = mkdir(path) #&& cp(from, s"$path/$filename") !
    path.split("/").lastOption.collect { case uuid if result == 0 =>
        dao.save(Attachment(new ObjectId(), uuid, userId, filename, size))
    }.exists(_.wasAcknowledged)
  }

  def find(uuid: String) = {
    dao.findOne(MongoDBObject("uuid" -> uuid))
  }

  def findByUserId(uuid: String, userId: Int) = {
    dao.findOne(MongoDBObject(
      "uuid" -> uuid,
      "user_id" -> userId
    ))
  }

  def findByUserId(userId: Int) = {
    dao.find(MongoDBObject(
      "user_id" -> userId
    )).toList
  }

  def remove(userId: Int, uuid: String, path: String): Option[Boolean] = {
    for {
      attachment <- findByUserId(uuid, userId)
      filename <- Try(filename(path)).toOption
    } yield {
      rm(s"$path/$filename") == 0 &&
      rm(path) == 0 &&
      remove(attachment).isUpdateOfExisting
    }
  }
}
