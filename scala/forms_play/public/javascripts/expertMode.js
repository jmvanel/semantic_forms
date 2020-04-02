const SF_ExpertModeKey = "SF_ExpertModeKey"

function getElementsExpertMode() {
  return document.getElementsByClassName('sf-local-rdf-link')
}

function applyExpertMode(isExpertMode) {
  const localRdfLinks = getElementsExpertMode()
  if(isExpertMode)
    visibility = "visible"
  else
    visibility = "hidden"
  for (var i = 0; i < localRdfLinks.length; i ++) {
    localRdfLinks[i].style.visibility = visibility
  }
}

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

