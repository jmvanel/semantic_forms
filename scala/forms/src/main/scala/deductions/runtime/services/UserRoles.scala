package deductions.runtime.services

/** cf https://github.com/jmvanel/semantic_forms/issues/209 */
object UserRoles {
  sealed trait UserRole
  case object NotLogged extends UserRole
  case object Logged extends UserRole
  case object Admin extends UserRole
  case object ContentManager extends UserRole
}