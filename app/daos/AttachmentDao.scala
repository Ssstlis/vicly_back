package daos

import scala.language.postfixOps
import scala.sys.process._

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

  def saveFile(from: String, path: String, filename: String, userId: Int, size: Long) = {
    val result = (mkdir(path) #&& cp(from, s"$path/$filename") !) == 0
    path.split("/").lastOption.collect { case uuid if result =>
        dao.save(Attachment(new ObjectId(), uuid, userId, filename, size))
    }.exists(_.wasAcknowledged)
  }

  def find(uuid: String) = {
    dao.findOne(MongoDBObject("uuid" -> uuid))
  }

  def findByUserId(userId: Int) = {
    dao.find(MongoDBObject(
      "user_id" -> userId
    )).toList
  }
}
