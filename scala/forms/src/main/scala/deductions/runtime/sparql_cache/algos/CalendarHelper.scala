package deductions.runtime.sparql_cache.algos

import java.util.Calendar
import java.text.SimpleDateFormat

/** NOTE see also for Joda: https://stackoverflow.com/questions/9307884/retrieve-current-weeks-mondays-date */
trait CalendarHelper {

  lazy private val dateFormatISO = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")

  /** enumerate days of current month in ISO format */
  protected def generateDaysOfMonth(date: Calendar): Seq[(String, String)] = {
    val (begin, end) = makeBeginEndOfDay(date)

    val daysInMonth = begin.getActualMaximum(Calendar.DAY_OF_MONTH)
    for (day <- 1 to daysInMonth) yield {
      val beginOfDay = cloneCalendar(begin)
      beginOfDay.set(Calendar.DAY_OF_MONTH, day)
      val endOfDay = cloneCalendar(end)
      endOfDay.set(Calendar.DAY_OF_MONTH, day)
      (formatISO(beginOfDay), formatISO(endOfDay))
    }
  }

  /** generate 2 Half Days */
  protected def generateHalfDays(date: Calendar, limit: Int = 16): Seq[(String, String)] = {
    val (beginOfDay, endOfDay) = makeBeginEndOfDay(date)
    val limitOfDay = cloneCalendar(beginOfDay)
    limitOfDay.set(Calendar.HOUR_OF_DAY, limit)
    Seq((formatISO(beginOfDay), formatISO(limitOfDay)),
      (formatISO(limitOfDay), formatISO(endOfDay)))
  }

  protected def formatISO(date: Calendar) = {
    dateFormatISO.format(date.getTime)
  }

  protected def formatReadable(date: Calendar) = {
    formatISO(date) // TODO better
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

  /** generate current Monday and monday Before */
  protected def generateMondays(date: Calendar) = {
    val monday = generateMonday(date)
    val mondayBefore = addDays(monday, -7)
    Seq(monday, mondayBefore)
  }

  protected def generateMonday(date: Calendar) = {
    val ret = cloneCalendar(date)
    ret.
      set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
    ret
  }

  protected def addDays(date: Calendar, daysCount: Int): Calendar = {
    val res = cloneCalendar(date)
    res.set(Calendar.DAY_OF_MONTH,
      date.get(Calendar.DAY_OF_MONTH) + daysCount)
    res
  }

  protected def cloneCalendar(date: Calendar) = date.clone().asInstanceOf[Calendar]

}