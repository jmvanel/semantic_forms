package deductions.runtime.sparql_cache.algos

import java.util.Calendar
import java.text.SimpleDateFormat

trait CalendarHelper {
  
    /** enumerate days of current month */
  protected def generateDaysOfMonth(date: Calendar) = {
    val (begin, end) = makeBeginEndOfDay(date)
    
    val daysInMonth = begin.getActualMaximum(Calendar.DAY_OF_MONTH)
    for( day <- 1 to daysInMonth) yield {
      val beginOfDay = cloneCalendar(begin)
      beginOfDay.set(Calendar.DAY_OF_MONTH, day)
      val endOfDay   = cloneCalendar(end)
      endOfDay.set(Calendar.DAY_OF_MONTH, day)
      val df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")
      (df.format(beginOfDay.getTime), df.format(endOfDay.getTime))
    } 
  }

  protected def makeBeginEndOfDay(begin: Calendar) = {
    begin.set(Calendar.HOUR_OF_DAY, 0)
    begin.set(Calendar.MINUTE, 0)
    begin.set(Calendar.SECOND, 0)
    begin.set(Calendar.MILLISECOND, 0)

    val end = cloneCalendar(begin)
    end.set(Calendar.HOUR_OF_DAY, 24)
    (begin, end)
  }

  protected def cloneCalendar(date: Calendar) = date.clone().asInstanceOf[Calendar]

}