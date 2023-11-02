# BackyardBan (BungeeCord Plugin)

#### Description:
***BackyardBan*** is a very basic BungeeCord plugin which allows Minecraft server moderators to ban players and ip adresses from the entire bungee network with optional expiration time as well as custom message.

![image](./illustrations/illustration.jpg)

#### Features:
- A *bypass* permission node is available to exempt players from beeing banned.
- A *ban* command is available to ban a player from the entire bungee network with an optional custom message and expiration time.
- A *banip* command is available to ban IP adress from the entire bungee network (taking both playername or direct IP adress as argument), with a custom message and expiration time.
- An *unban* command is available to unban both IP adress and playername. The *pardon* command is an alias of *unban*.
- A *reload* command is available to reload configuration and localization data.
- A *version* command is available to show plugin version and informations.
- A *help* command is available to show the help page, containing command list.
- Initial *localization* strings (English and French) are available, to customize ban messages.

---

#### Permissions:
```backyardban.ban``` - Allows to use the ```/ban``` and ```/backyardban:ban``` commands.<br/>
```backyardban.banip``` - Allows to use the ```/banip``` and ```/backyardban:banip``` commands.<br/>
```backyardban.unban``` - Allows to use the ```/unban```, ```/pardon``` and ```/backyardban:unban``` commands.<br/>
```backyardban.bypass``` - Allows player to be insensitive to ```/ban``` or ```/banip``` command.<br/>
```backyardban.reload``` - Allows player to reload config files using ```/backyardban:reload``` command.<br/>

#### Usage:
```/ban [playername] {t:(1h|1d|1m|1y)} (reason)``` - Ban player with a message.<br/>
```/banip [ip|playername] {t:(1h|1d|1m|1y)} (reason)``` - Ban IP with a message.<br/>
```/unban [ip|playername]``` - Unban IP or player.<br/>
```/backyardban:help``` - Show the help page.<br/>
```/backyardban:reload``` - Reload the configuration files.<br/>
```/backyardban:version``` - Show plugin version.<br/>

#### Examples:
```/ban IndividuLambda``` - Bans player ```IndividuLambda``` forever with no reason.<br />
```/ban IndividuLambda Get out!``` - Bans the player with the reason "Get out!".<br />
```/ban IndividuLambda t:7d Get out!``` - Bans the player for 1 week.<br />
```/banip IndividuLambda t:7m Get all out!``` - Bans the IP adress for 1 month.<br />
```/banip 56.79.10.10``` - Bans this IP adress forever with no reason. <br />
```/unban IndividuLambda``` - Unbans the player ```IndividuLambda``` as well as his IP adress. <br />
```/unban 56.79.10.10``` - Unbans specific IP adress.


---

*Feel free to give me feedback using the Issues tab !*<br/>
*Augustin Blanchet*
