rootProject.name = "msa"

include("config", "eureka", "firm", "gateway", "license")

project(":config").projectDir = file("service/config")
project(":eureka").projectDir = file("service/eureka")
project(":firm").projectDir = file("service/firm")
project(":gateway").projectDir = file("service/gateway")
project(":license").projectDir = file("service/license")
include("resillience")
include("service:resillience")