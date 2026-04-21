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
3. Configure a chave JWT (minimo 32 bytes):
	```sh
	cp .env.example .env
	```

	Edite o arquivo `.env` e defina um valor forte para `JWT_SECRET`.

	Opcionalmente, voce pode definir via variavel de ambiente:
	```sh
	export JWT_SECRET='troque_por_uma_chave_forte_com_32_ou_mais_bytes'
	```
4. Compile e execute o projeto com Maven:
	```sh
	mvn clean package
	java -cp target/PjBL-Auth-1.0.jar com.pucpr.Main
	```

Alternativa sem export no shell:
```sh
java -DJWT_SECRET='troque_por_uma_chave_forte_com_32_ou_mais_bytes' -cp target/PjBL-Auth-1.0.jar com.pucpr.Main
```

## Execução do Frontend
Abra o arquivo `Frontend/index.html` em seu navegador.
