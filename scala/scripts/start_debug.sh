echo 'Run SF with JVM debugging activated'
export SBT_OPTS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=9999" && sbt
