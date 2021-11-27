const SF_ExpertModeKey = "SF_ExpertModeKey"

function getElementsExpertMode() {
  return document.getElementsByClassName('sf-local-rdf-link')
}

/** set visibility according to Expert Mode flag from local Storage */
function applyExpertMode(isExpertMode) {
  const localRdfLinks = getElementsExpertMode()
  if(isExpertMode)
    visibility = "inline" // "visible"
  else
    visibility = "none" // "hidden"
  for (var i = 0; i < localRdfLinks.length; i ++) {
    // localRdfLinks[i].style.visibility = visibility
    localRdfLinks[i].style.display = visibility
  }
  var expertModeCheckbox = document.getElementById("expertModeCheckbox")
  expertModeCheckbox.checked = isExpertMode
}

/** get Expert Mode flag from local Storage */
function getExpertMode() {
  const isExpertModeString = localStorage.getItem(SF_ExpertModeKey)
  return String(isExpertModeString).toLowerCase() === 'true'
}

function checkExpertModeOnPageLoad() {
  const isExpertMode = getExpertMode()
  applyExpertMode(isExpertMode)
  console.log("applied Expert Mode(" + isExpertMode + ") on Page Load" )
}

function toggleExpertMode() {
  const isExpertMode = getExpertMode()
  const value = ! isExpertMode
  localStorage.setItem(SF_ExpertModeKey, value)
  applyExpertMode(value)
  console.log("applied Expert Mode(" + value + ")" )
}

