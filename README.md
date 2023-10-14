# 5c-client-N

This is a fully open source version of the Fifth column's client N decompiled by brownman_20

You can have full access to N "client" the Fifth columns "private" client

## Features:
- to be documented...

### Main branch (BSB Edition)
- New assets including some shitposts that would be seen in title screen and some other easter eggs
- Communication with 5C services is disabled by default to prevent leaking your IP and other data to the Fifth column.
    - You can enable 5C services in source code by setting `COPE_OFFLINE_MODE` to `false` in NMod class and then rebuilding the jar.
    - The service most likely won't work because you are probably not an authenticated user on their servers. Unless you want to make your own Copenheimer clone :)

### Unmod branch (Original Code)
- Not much modifications other than fixing errors from decompiler
- This version connects to online services owned by Fifth Column and might leak your IP address and other Telemetry about what you are doing in game to the Fifth Column. Please use with caution.

## Usage/Installation guide
To use this mod you need to use "Minecraft 1.20.1" with Fabric as your mod loader and you should also install additional mods listed below:
- Fabric API
- Meteor client
  - Since this mod is a Meteor client add-on, you can only use it with Meteor client.

## Building guide
- Use gradle build task in your IDE (such as Intellij IDEA) to build the jar. You can find it be in `build/libs/` directory.
- Can also be built in command line by using `./gradlew build` command

DMCA if ur a pedophile