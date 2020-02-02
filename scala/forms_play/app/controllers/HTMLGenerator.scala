package controllers

import deductions.runtime.services.html.Form2HTMLObject
import deductions.runtime.utils.FormModuleBanana
import deductions.runtime.jena.ImplementationSettings

trait HTMLGenerator extends FormModuleBanana[ImplementationSettings.Rdf] {
  val config: deductions.runtime.utils.Configuration
  //  override lazy 
  val htmlGenerator =
    Form2HTMLObject.makeDefaultForm2HTML(config)(ops)
  }