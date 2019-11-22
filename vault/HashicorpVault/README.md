# Start the Vault

To start the [vault server](https://learn.hashicorp.com/vault/getting-started/deploy) run:

```powershell
vault server -config ./vault.hcl
```

# Initialize the Vault

The Vault was initialized with:

```powershell
$Env:VAULT_ADDR="https://www.gz.com:8200"
$Env:VAULT_CACERT="./vault-config/euge.pem"
$Env:VAULT_API_ADDR="https://www.gz.com:8200"
```

```powershell
vault operator init
```

It creates the following shared keys:

```sh
Unseal Key 1: zmocsW1BNxscgy6QauyvJ3cWHyj+rMkgnEw4F/CjqmsC
Unseal Key 2: wkU887b9SZ7ftwvCrUO7svVMPXXu2Lho2StfOQbRR9fo
Unseal Key 3: GZRLclX+PW4mh5xd0Ll1Rl9JIyOi4KjrJxhZnc0W6/g4
Unseal Key 4: 5CA4YzhfLiZ3nCjD/DQXBhInEFwKMQAv2Fa5bQ/SL6bE
Unseal Key 5: zNBV4800O+Pu2Lg6j4Dl3+7KPCUJaczmak3hS3VgjGvV

Initial Root Token: s.dremNR6XWrPRj1gQrQwlyrWN
```

We set the root token on an environment variable to ease the execution of command line comands:

```powershell
$Env:VAULT_TOKEN="s.dremNR6XWrPRj1gQrQwlyrWN"
```

## Dev mode

If the vault has started in developer mode with 'vault server -dev`, the environment variable to set is:

```sh
$Env:VAULT_DEV_ROOT_TOKEN_ID="s.dremNR6XWrPRj1gQrQwlyrWN"
```

In this case the vault would be unsealed, and the backend is in memory - inmem.

# Unseal the Vault

We can see the vault status:

```powershell
vault status

Key                Value
---                -----
Seal Type          shamir
Initialized        true
Sealed             true
Total Shares       5
Threshold          3
Unseal Progress    0/3
Unseal Nonce       n/a
Version            1.2.3
HA Enabled         false
```

We proceed now to `unseal` the vault. We are going to need three of the `shared keys`. This will let the `vault barrier` to discover which is the `root key` and decrypt the data from the vault:

```powershell
vault operator unseal zmocsW1BNxscgy6QauyvJ3cWHyj+rMkgnEw4F/CjqmsC

Key                Value
---                -----
Seal Type          shamir
Initialized        true
Sealed             true
Total Shares       5
Threshold          3
Unseal Progress    1/3
Unseal Nonce       000431e1-5cce-0e3e-cb11-d7dfc74a0c3e
Version            1.2.3
HA Enabled         false
```

```powershell
vault operator unseal wkU887b9SZ7ftwvCrUO7svVMPXXu2Lho2StfOQbRR9fo

Key                Value
---                -----
Seal Type          shamir
Initialized        true
Sealed             true
Total Shares       5
Threshold          3
Unseal Progress    2/3
Unseal Nonce       000431e1-5cce-0e3e-cb11-d7dfc74a0c3e
Version            1.2.3
HA Enabled         false
```

```powershell
vault operator unseal GZRLclX+PW4mh5xd0Ll1Rl9JIyOi4KjrJxhZnc0W6/g4

Key             Value
---             -----
Seal Type       shamir
Initialized     true
Sealed          false
Total Shares    5
Threshold       3
Version         1.2.3
Cluster Name    vault-cluster-2f534401
Cluster ID      fd7b874b-0959-4832-3cf3-0c02f69bb540
HA Enabled      false
```

# Creating a Secret

## Enable a KV engine

We enable the `kv engine`:

```sh
vault secrets enable -path=secret/ kv
```

We can also disable it:

```sh
vault secrets disable kv/
```

## KV secrets

### Create

We can create a secret:

```sh
vault kv put secret/hello foo=world
```

We can create more than one secret at a time:

```sh
vault kv put secret/hello foo=world excited=yes
```

### Retrieve

We can retrieve a secret

```sh
vault kv get secret/hello

===== Data =====
Key        Value
---        -----
excited    yes
foo        world
```

If we just want to get the value of a key:

```sh
vault kv get -field=excited secret/hello

yes
```

We can also retrieve the value as a `json`:

```sh
vault kv get -format=json secret/hello 

{
  "request_id": "ebc3e1ad-eab4-844a-b606-5b2383b67a56",
  "lease_id": "",
  "lease_duration": 2764800,
  "renewable": false,
  "data": {
    "excited": "yes",
    "foo": "world"
  },
  "warnings": null
}
```

We can fetch a value from the `json`

```sh
vault kv get -format=json secret/hello | jq -r .data.data.excited
```

### List secrets

```sh
vault kv list secret/gs-vault-config/
```

### Delete a secret

```sh
vault kv delete secret/hello
```

# Secrets Engine

Vault behaves similarly to a virtual filesystem. The read/write/delete/list operations are forwarded to the corresponding secrets engine, and the secrets engine decides how to react to those operations.

This abstraction enables Vault to interface directly with physical systems, databases, HSMs, etc. But in addition to these physical systems, Vault can interact with more unique environments like AWS IAM, dynamic SQL user creation, etc. all while using the same read/write interface.

Previously, we saw how to read and write arbitrary secrets to Vault. You may have noticed all requests started with `secret/`.

The path prefix tells Vault which secrets engine to which it should route traffic. When a request comes to Vault, __it matches the initial path part using a longest prefix match__ and then passes the request to the corresponding secrets engine enabled at that path.

__By default, Vault enables a secrets engine called kv at the path secret/__. The kv secrets engine reads and writes raw data to the backend storage.

We can enable another instance of the kv secrets engine at a different path. Just like a filesystem, Vault can enable a secrets engine at many different paths. Each path is completely isolated and cannot talk to other paths. For example, a kv secrets engine enabled at foo has no ability to communicate with a kv secrets engine enabled at bar.

```sh
vault secrets enable -path=euge kv
```

```sh
vault kv put euge/hello foo=world excited=yes
```

```sh
vault kv get euge/hello

===== Data =====
Key        Value
---        -----
excited    yes
foo        world
```

Vault supports many other secrets engines besides kv, and this feature makes Vault flexible and unique. For example, the aws secrets engine generates AWS IAM access keys on demand. The database secrets engine generates on-demand, time-limited database credentials. These are just a few examples of the many available secrets engines.

```sh
vault secrets enable -path=aws aws
```

We can create a secret:
```sh
vault write aws/config/root \
    access_key=AKIAI4SGLQPBX6CSENIQ \
    secret_key=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 \
    region=us-east-1
```

## Engines Defined
 
We can see all the engines defined:

```sh
vault secrets list

Path          Type         Accessor              Description
----          ----         --------              -----------
cubbyhole/    cubbyhole    cubbyhole_7107cc7c    per-token private secret storage
euge/         kv           kv_35f7dc0c           n/a
identity/     identity     identity_d810486a     identity store
secret/       kv           kv_da9f030b           n/a
sys/          system       system_a81e1328       system endpoints used for control, policy and debugging
```

## Dynamic Credentials

This credentials are created on demand. 

```sh
vault secrets enable database

vault write database/config/mysql-fakebank \
	plugin_name=mysql-legacy-database-plugin \
	connection_url="{{username}}:{{password}}@tcp(127.0.0.1:3306)/fakebank" \
	allowed_roles="*" \
	username="fakebank-admin" \
	password="Sup&rSecre7!"
```

# Tokens

[Authentication documentation](https://learn.hashicorp.com/vault/getting-started/authentication)

## Create a Token

Lets create a new token that will expire in 40 mins:

```sh
vault token create -ttl 40m

Key                  Value
---                  -----
token                s.HPbHU9G2ZwLGEzbC78TmG0BJ
token_accessor       fcOlXWgEmXwdPIBn7pkLsyoV
token_duration       40m
token_renewable      true
token_policies       ["root"]
identity_policies    []
policies             ["root"]
```

## Log with a Token

We log in using this token:

```sh
vault login

Token (will be hidden): 

Success! You are now authenticated. The token information displayed below
is already stored in the token helper. You do NOT need to run "vault login"
again. Future Vault requests will automatically use this token.

Key                  Value
---                  -----
token                s.HPbHU9G2ZwLGEzbC78TmG0BJ
token_accessor       fcOlXWgEmXwdPIBn7pkLsyoV
token_duration       38m41s
token_renewable      true
token_policies       ["root"]
identity_policies    []
policies             ["root"]
```

After the `ttl`, the token will be deactivated.

## Look up a Token

Podemos ver la definicion del token:

```sh
vault token lookup s.HPbHU9G2ZwLGEzbC78TmG0BJ                     

Key                 Value
---                 -----
accessor            fcOlXWgEmXwdPIBn7pkLsyoV
creation_time       1573924362
creation_ttl        40m
display_name        token
entity_id           n/a
expire_time         2019-11-16T18:52:42.8661674+01:00
explicit_max_ttl    0s
id                  s.HPbHU9G2ZwLGEzbC78TmG0BJ
issue_time          2019-11-16T18:12:42.8661674+01:00
meta                <nil>
num_uses            0
orphan              false
path                auth/token/create
policies            [root]
renewable           true
ttl                 29m20s
type                service
```

## Renew a Token

We can renew the token, so that the `ttl` is reset:

```sh
vault token renew s.HPbHU9G2ZwLGEzbC78TmG0BJ

Key                  Value
---                  -----
token                s.HPbHU9G2ZwLGEzbC78TmG0BJ
token_accessor       fcOlXWgEmXwdPIBn7pkLsyoV
token_duration       40m
token_renewable      true
token_policies       ["root"]
identity_policies    []
policies             ["root"]
```

## Revoke a Token

```sh
vault token revoke s.HPbHU9G2ZwLGEzbC78TmG0BJ
```

# Policies

Definimos  una [policy](https://learn.hashicorp.com/vault/getting-started/policies) en el archivo `./sample-policy.hcl`

```yml
path "euge/hello" {
    capabilities = ["read"]
}
```

Ahora creamos la policy usando el archivo, y la _submimos_ al vault:

```sh
vault policy write mipolicy-ro ./sample-policy.hcl
```

Podemos ver como entre las policies ya figura la policy que acabamos de crear:

```sh
vault policy list                                                 

default
mipolicy-ro
root
```

Utilizar la policy significa que la asignemos a token(s):

```sh
vault token create -policy=mipolicy-ro -ttl 40m

Key                  Value
---                  -----
token                s.gx7EakOYHIRep2gmPfbct2k6
token_accessor       jcL0swEUfPlKXqV0awrY5e56
token_duration       40m
token_renewable      true
token_policies       ["default" "mipolicy-ro"]
identity_policies    []
policies             ["default" "mipolicy-ro"]
```

Cuando hagamos login con este token podremos acceder a los secretos guardados en `euge/hello`

# Using the HTTP APIs

[Http API](https://learn.hashicorp.com/vault/getting-started/apis)
