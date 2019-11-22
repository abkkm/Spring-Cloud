api_addr = "http://www.gz.com:8200"
log_level = "trace"

storage "consul" {
  address = "10.0.75.1:8500"
  path    = "vault"
  service = "vault"
}

listener "tcp" {
  address     = "www.gz.com:8200"
  tls_disable = 1
}

telemetry {
  statsite_address = "www.gz.com:8125"
  disable_hostname = true
}
