package services.mongodb
import org.bson.types.ObjectId
import com.mongodb.casbah.commons.MongoDBObject
import play.api.Logger
import models._
import com.novus.salat.dao.{ModelCompanion, SalatDAO}
import MongoContext.context
import play.api.Play.current
import com.mongodb.casbah.Imports._
import play.api.libs.json.JsValue
import javax.inject.{Inject, Singleton}
import models.Preview
import scala.Some
import play.api.libs.json.JsObject
import com.mongodb.casbah.commons.TypeImports.ObjectId
import com.mongodb.casbah.WriteConcern
import play.api.libs.json.Json
import services.MetadataService
import play.api.libs.functional.syntax._
import play.api.libs.json.Writes
import play.api.libs.json.JsPath

@Singleton
class MongoDBMetadataService extends MetadataService{
  /** Add metadata to the metadata collection and attach to a section /file/dataset/collection */
  def addMetadata(metadata: Metadata): UUID = {
    Logger.debug("Inside addMetadata")
    val mid = MetadataDAO.insert(metadata, WriteConcern.Safe)
    val id = UUID(mid.get.toString())
    id
  }

  def getMetadataById(id: UUID): Option[Metadata] = {
    MetadataDAO.findOneById(new ObjectId(id.stringify)) match {
      case Some(metadata) => {
        //TODO link to context based on context id
        Some(metadata)
      }
      case None => None
    }
  }

  /** Get Metadata based on Id of an element (section/file/dataset/collection) */
  def getMetadataByAttachTo(elementType: String, elementId: UUID): List[Metadata] = {
    //val eidMap = Map(elementType + "_id" -> new ObjectId(elementId.stringify))
    // val MetadataAttachedTo  = MetadataDAO.find(MongoDBObject("attachedTo" -> eidMap))
    val MetadataAttachedTo  = MetadataDAO.find(MongoDBObject(("attachedTo."+ elementType + "_id") -> new ObjectId(elementId.stringify)))
    var md : List[Metadata]= List.empty
    for (e <- MetadataAttachedTo) {
      md = e :: md
    }
    md
  }

  /** Get metadata based on type i.e. user generated metadata or technical metadata  */
  def getMetadataByCreator(elementType: String, elementId: UUID, typeofAgent: String): List[Metadata] = {
    //val eidMap = Map(elementType + "_id" -> new ObjectId(elementId.stringify))
    val mdlistForElementType = MetadataDAO.find(MongoDBObject("attachedTo"+"."+ elementType +"_id" ->new ObjectId(elementId.stringify)))
    var mdlist: List[Metadata] = List.empty
    for (md <- mdlistForElementType) {
      var creator = md.creator
      if (creator.typeOfAgent == typeofAgent) {
        mdlist = md :: mdlist
      }
    }
    mdlist
  }
  /** Remove metadata */
  def removeMetadata(id: UUID) = {
    val md = getMetadataById(id).getOrElse(null)
    MetadataDAO.remove(md, WriteConcern.Safe)
  }
  
  /** Get metadata context if available 
   * TODO: Implement this when context is defined 
   *  */
  def getMetadataContext(metadataId: UUID): Option[JsValue] = { None}
  
  /** update Metadata 
   *  TODO
   *  */  
  def updateMetadata(metadataId: UUID, json: JsValue) = {}

}
object MetadataDAO extends ModelCompanion[Metadata, ObjectId] {
  // TODO RK handle exception for instance if we switch to other DB
  val dao = current.plugin[MongoSalatPlugin] match {
    case None => throw new RuntimeException("No MongoSalatPlugin")
    case Some(x) => new SalatDAO[Metadata, ObjectId](collection = x.collection("metadata")) {}
  }
}