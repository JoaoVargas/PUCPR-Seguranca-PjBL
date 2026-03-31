# PjBL1 Seg

## Execução do Backend
1. Navegue até a pasta do backend:
	```sh
	cd Backend/PjBL-Auth
	```
2. Caso tenha JENV configurado execute, se não só ignorar:
	```sh
	jenv local $(cat ./.java-version)
	```
3. Compile e execute o projeto com Maven:
	```sh
	mvn clean package
	java -cp target/PjBL-Auth-1.0-SNAPSHOT.jar com.pucpr.Main
	```

## Execução do Frontend
Abra o arquivo `Frontend/index.html` em seu navegador.
