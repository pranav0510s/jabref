tell application "Finder"
  set theDisk to a reference to (disks whose URL = "DEPLOY_VOLUME_URL")
  open theDisk

  set theWindow to a reference to (container window of disks whose URL = "DEPLOY_VOLUME_URL")

  set current view of theWindow to icon view
  set toolbar visible of theWindow to false
  set statusbar visible of theWindow to false

  -- size of window should fit the size of background
  set the bounds of theWindow to {346, 100, 920, 500}

  set theViewOptions to a reference to the icon view options of theWindow
  set arrangement of theViewOptions to not arranged
  set icon size of theViewOptions to 128
  set background picture of theViewOptions to POSIX file "DEPLOY_BG_FILE"

  -- Create alias for install location
  make new alias file at POSIX file "DEPLOY_VOLUME_PATH" to POSIX file "DEPLOY_INSTALL_LOCATION" with properties {name:"DEPLOY_INSTALL_LOCATION_DISPLAY_NAME"}

  set allTheFiles to the name of every item of theWindow
  set xpos to 120
  repeat with theFile in allTheFiles
    set theFilePath to POSIX path of theFile
    set appFilePath to POSIX path of "/DEPLOY_TARGET"
    if theFilePath ends with "DEPLOY_INSTALL_LOCATION_DISPLAY_NAME" then
      -- Position install location application
      set position of item theFile of theWindow to {440, 170}
    else if theFilePath ends with appFilePath then
      -- Position application or runtime
      set position of item theFile of theWindow to {140, 170}
    else
      -- Position all other items in a second row.
      set position of item theFile of theWindow to {xpos, 400}
      set xpos to xpos + 150
    end if
  end repeat


  update theDisk without registering applications
  delay 5
  close (get window of theDisk)
end tell
