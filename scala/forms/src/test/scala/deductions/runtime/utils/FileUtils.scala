package deductions.runtime.utils

import java.io.IOException
import java.nio.file.FileSystems
import java.nio.file.FileVisitResult
import java.nio.file.FileVisitor
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes

object Fil‍eUtils {

  def deleteLocalSPARL() = {
    deleteRecursive("TDB")
  }
  def deleteRecursive(dir: String) = {
    val path = FileSystems.getDefault.getPath(dir)
    if (Files.exists(path) && Files.isDirectory(path)) {
      Files.walkFileTree(path, new FileVisitor[Path] {
        def visitFileFailed(file: Path, exc: IOException) = FileVisitResult.CONTINUE
        def visitFile(file: Path, attrs: BasicFileAttributes) = {
          Files.delete(file)
          FileVisitResult.CONTINUE
        }
        def preVisitDirectory(dir: Path, attrs: BasicFileAttributes) = FileVisitResult.CONTINUE
        def postVisitDirectory(dir: Path, exc: IOException) = {
          Files.delete(dir)
          FileVisitResult.CONTINUE
        }
      })
    }
  }

}