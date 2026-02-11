FROM grafana/promtail:latest

# Copy promtail configuration
COPY promtail-config.yml /etc/promtail/config.yml

# Start Promtail with config file
CMD ["-config.file=/etc/promtail/config.yml"]
