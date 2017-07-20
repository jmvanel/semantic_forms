package deductions.runtime.mobion

case class VehicleStatistics(
    begin: String,
    end: String,
    averageDistance: Float,
    totalDistance: Float,
    averageTime: String = "",
    totalTime: String = ""
    )