#!/bin/sh
set -e

# Genera la configuracion runtime del frontend a partir de las variables de entorno.
# Protocolo/host vacios => la app los deduce de window.location (sirve en cualquier LAN).
envsubst < /etc/nginx/templates/config.template.json > /usr/share/nginx/html/config.json
