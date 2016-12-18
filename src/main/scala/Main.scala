package main.scala

import java.io.{File, PrintWriter}
import java.nio.file.Files

import scala.io.Source

case class User(name: String, gender: Option[String])

object Main {
  def main(args: Array[String]): Unit = {
    val location = "/Users/abu/IdeaProjects/exante/backoffice-client"
    val lFile = new File(location)
    if (!lFile.isDirectory) throw new Exception("directory expected")
    processFile(lFile)
  }

  private val twitterImport = "\\s*import com.twitter.util\\.(.*)".r
  private val twitterImports = "\\s*import com.twitter.util\\.\\{(.*)\\}".r

  private def processFile(f: File): Unit = {
    if (f.isDirectory) f.listFiles().foreach(processFile)
    else if (f.getName.endsWith("scala")) {

      val requiredReplaces = Source.fromFile(f).getLines().collect {
        case twitterImports(classes) => classes.split(",").map(_.trim)
        case twitterImport(aClass) => Array(aClass)
      }.flatten.toSet

      if (requiredReplaces.contains("_")) {
        println(s"cant process '${f.getName}': contains wildcard import")
      } else if(requiredReplaces.nonEmpty) {
        val oldFilePath = Files.move(f.toPath, f.toPath.resolveSibling(s"${f.getName}.old"))
        val pw = new PrintWriter(f)

        var exportReplaced = false
        Source.fromFile(oldFilePath.toFile).getLines().foreach {
          case twitterImport(_) => // matches both single and multiply class exports
            if(!exportReplaced) {
              exportReplaced = true
              pw.println("import com.twitter.{util => twitter}")
            }
          case line =>
            pw.println(line.split(" ").map {
              it => if (requiredReplaces.exists(r => it.startsWith(r))) s"twitter.$it" else it
            }.mkString(" "))
        }
        pw.close()
        oldFilePath.toFile.delete()
      }
    }
  }
}
