# Overview

This project is a simple iptv restreamer. For now it supports only HLS (m3u8) streams.
Some iptv providers allow to connect only one device per url and this is not really
comfortable when you have 3+ tv. Iptv-proxy allocates such 'urls' dynamically. I.e. your
iptv provider have two urls (playlists) and allows only one connection per url, but
you have 4 tv in your house and you never watch more then 2 tv at the same time.
In this case you can setup two playlists in iptv proxy and they will be dynamically
allocated to active tv.

## Configuration

```yaml
host: 127.0.0.1
port: 8080
base_url: http://127.0.0.1:8080
token_salt: 6r8bt67ta5e87tg7afn
timeout_sec: 30
servers:
  - name: someiptv-1
    url: https://someiptv.com/playlist.m3u
    max_connections: 1
  - name: another_proxy-2
    url: https://iptv-proxy.example.com/playlist.m3u
    max_connections: 4
    send_user: true
allow_anonymous: false
users:
  - 65182_login1
  - 97897_login2
```

* `base_url` - url of your service
* `token_salt` - just random chars, they are used to create encrypted tokens
* `max_connections` - max active connections allowed for this playlist
* `send_user` - this is useful only when you're using cascade config - iptv-proxy behind iptv-proxy.
If 'true' then iptv-proxy will send current user name in special http header.
We need this to identify device (endpoint) - this will help us to handle max connections and
channel switching properly.
* `allow_anonymous` - allow to connect any device without specific user name.
It is not good idea to use such setup. You really should add name for each device you're using.

## Device setup

On device you should use next url as dynamic playlist:

`<base_url>/m3u/<user_name>`

or

`<base_url>/m3u`

for anonymous access.
