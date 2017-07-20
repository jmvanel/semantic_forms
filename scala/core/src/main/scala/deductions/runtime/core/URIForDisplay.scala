package deductions.runtime.core

case class URIForDisplay(uri: String, label: String, typ: String, typeLabel: String,
   thumbnail: Option[String],
   isImage: Boolean )