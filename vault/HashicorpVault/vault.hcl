storage "file" {
  path = "./vault-data"
}
listener "tcp" {
  address = "www.gz.com:8200"
  tls_cert_file = "./vault-config/euge.pem"
  tls_key_file = "./vault-config/euge.key"
}