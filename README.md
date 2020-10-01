# Notes for those reviewing this project

I developed this using Http4s Blaze Server and Cats Effect IO which is the first time I've developed an app using those frameworks.
Most of my previous experience is using Play, Akka and Futures. 

I have implmented all the functionality required but I do need to point out that due to time limitations and time learning to use Http4s I have not integrated this software with a DB but used in memory maps in the repository classes. 

If the DB is a required part of this excercise let me know and I will update the Repository classes to use a DB. 

I did not get time to sign the tokens or hash the passwords, either so let me know if you need this done. 

There are also TODO's scattered around the code for things I simply did not get time to implement but should give an idea of how I would implement this for a production system

Right now the app initialises 2 users for testing when it starts up:
* id: 1, username: "hob", password: "bob", friendId: 2
* id: 2, username: "poj", password: "joj", friendId: 1
This is done in UserRepository.scala if you want to change it. 
 
There is insufficient Unit testing of the services and auth functions but resonably good tests of the API's
 
## How it works

In order to solve the chicken and egg problem I had to bend rule 7 a little, you can get a permissionToken with the right credentials and no friend permission but getting an auth token(actually logging in) requires the permission token from a friend. 

When authenticating with the "/login" URL you specify username and password and you can either supply a bearer token or not.
If you do not supply a valid bearer token to the Login API but do supply valid credentials the API will return a "permissionToken" but no "authToken". 

If you supply a valid "permissionToken" from your "friend" to the Login API in the form of a "bearer" token in the "Authorization" header and valid credentials the API will return both a "permissionToken" and an "authToken"

All Secret related API calls require a valid "authToken" to be passed to the API in the form of a "bearer" token in the "Authorization" header.

## To Run
In the project root
```
$ sbt run
```

## To Run Tests
In the project root
```
$ sbt test 
```

## To view the API design
Please open the file "api-definition/sociallogin.yml" in https://editor.swagger.io/

## To test
To make it easier to test you can import the "Social Login.postman_collection.json" into Postman. 