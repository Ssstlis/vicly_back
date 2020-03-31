package model

final case class SeaweedResponse(
    id: Int,
    eTag: String,
    fileId: Int,
    fileName: String,
    fileUrl: String,
    fileSize: Int
)
