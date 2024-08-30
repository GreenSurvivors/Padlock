***WARNING: Versions for Minecraft 1.20.x are not fully tested yet.***
Please create an issus if you find anything wrong. Thank you.

All versions can be found at [Github Release Page](https://github.com/GreenSurvivors/Padlock/releases).

### Padlock - A much better Lockette(Pro) plugin for Paper

Padlock is a block-protection plugin based on LockettePro witch itself is based on
Lockette (https://github.com/Acru/Lockette), the code base is entirely re-written.
It will be 80% backwards compatible with Lockette(Pro) but do to how it saves all the data does not support downgrading
back to Lockette(Pro).

### Padlock has a lot of enhancements compared to Lockette(Pro):

1. Much better codebase and performance, fixed a lot of issues and glitches.
2. Real UUID support.
3. All blocks can be set to lockable or not. You can even prevent players from locking a chest.
4. Alert messages are editable, and UTF-8 characters are supported.
5. Lock expircy feature, locks can expire after a certain amount of time.
6. All propertys like members, owners, timers, everyone access is saved in persistentDataContainer. This way we do not
   need to hide anything via ProtocollLib, witch has proven to be error prone and hard to work with.
7. Allowing tags in config for future proofing. Want to protect all doors not having to update everytime mojang adds a
   new one? We got you covert!
8. Don't support Spigot or further upstream anymore. Is this really a pro argument? Yes, since this enables us to use
   cutting edge API and no longer get slowed down by supporting old code.

   However, there are a few incompatiblities:
   1. No Spigot support.
   2. No support for offline server (open a PR if you need support)
   3. Some removed API (open an issue if you miss anything)
