package daos

import com.google.inject.{Inject, Singleton}
import com.mongodb.casbah.commons.MongoDBObject
import models.Attachment
import org.bson.types.ObjectId
import ru.tochkak.plugin.salat.PlaySalat
import salat.dao.{ModelCompanion, SalatDAO}

import scala.language.postfixOps

@Singleton
class AttachmentDao @Inject()(
  mongoContext: MongoContext,
  playSalat: PlaySalat
) extends ModelCompanion[Attachment, ObjectId] {

  import mongoContext._

  val dao = new SalatDAO[Attachment, ObjectId](playSalat.collection("attachment", "ms")) {}

  //  def saveFile(from: String, path: String, filename: String, userId: Int, size: Long) = {
  //    val result = mkdir(path) #&& cp(from, s"$path/$filename") !
  //    path.split("/").lastOption.collect { case uuid if result == 0 =>
  //        dao.save(Attachment(new ObjectId(), uuid, userId, filename, size))
  //    }.exists(_.wasAcknowledged)
  //  }

  def saveFile(fid: String, filename: String, userId: Int, size: Long, isAvatar: Boolean, metadata: Map[String, String], mime: String, previewSmall: Option[Attachment] = None, previewBig: Option[Attachment] = None) = {
    val result = dao.insert(Attachment(new ObjectId(), fid, userId, filename, size, isAvatar, mime,metadata, previewSmall = previewSmall, previewBig = previewBig))
    result.flatMap { objectId =>
      dao.findOneById(objectId)
    }
  }

  def saveAttachment(attachment: Attachment) = {
    val result = dao.insert(attachment)
    result.flatMap { objectId =>
      dao.findOneById(objectId)
    }
  }

  def updateMetaAndPreview(attachment: Attachment, metadata: Map[String, String], previewSmall: Option[Attachment], previewBig: Option[Attachment]) = {
    if (dao.update(MongoDBObject(
      "_id" -> attachment._id
    ), MongoDBObject(
      "$set" -> MongoDBObject(
        "metadata" -> metadata,
        "previewSmall" -> previewSmall,
        "previewBig" -> previewBig
      ))).isUpdateOfExisting) {
      dao.findOneById(attachment._id)
    } else None
  }

  def find(id: String) = {
    try {
      val objectId = new ObjectId(id)
      dao.findOne(MongoDBObject("_id" -> objectId))
    } catch {
      case _: IllegalArgumentException => None
    }
  }

  def find(id: ObjectId) = {
    dao.findOne(MongoDBObject(
      "_id" -> id
    ))
  }

  def findById(id: String) = {
    dao.findOne(MongoDBObject(
      "_id" -> new ObjectId(id)
    ))
  }

  def findByUserId(userId: Int) = {
    dao.find(MongoDBObject(
      "user_id" -> userId
    )).toList
  }

  def remove(id: String): Boolean = {
    dao.remove(MongoDBObject(
      "_id" -> id
    )).wasAcknowledged()
  }
}
