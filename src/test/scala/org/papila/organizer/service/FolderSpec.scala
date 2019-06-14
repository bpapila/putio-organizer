package org.papila.organizer.service

import org.papila.organizer.service.Organizer.Folder
import org.scalatest.FlatSpec
import org.scalatest.Matchers._

class FolderSpec extends FlatSpec {

  import FolderFixtures._

  "addSubFolder" should "add subfolder if not exist" in {
    RootFolder.addSubFolder(SubFolder) shouldBe RootWithSub
  }

  "addSubFolder" should "return folder as is if subfolder exist" in {
    RootWithSub.addSubFolder(SubFolder) shouldBe RootWithSub
  }

  "hasSubFolder" should "return true if folder found" in {
    RootWithSub.hasSubFolder(SubFolder.name) shouldBe true
  }

  "hasSubFolder" should "return false if folder found" in {
    RootFolder.hasSubFolder(SubFolder.name) shouldBe false
  }

}

object FolderFixtures {

  val RootFolder = Folder("root", 1)
  val SubFolder = Folder("sub", 10)

  val RootWithSub = Folder("root", 1, Map("sub" -> Folder("sub", 10)))

}