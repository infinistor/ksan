# Core-Util

## ksanFSCK
It is an object consistancy checking and fixing utility
### Usage 
```sh
ksanFSCK --help
 --bucketName VAL : Specify the name of the bucket you wish to fix or check
 --checkOnly      : Specify if you wish only to check not to fix
 --diskId VAL     : Specify the disk Id you wish to fix or check
 --help           : To display this help menu
 --target VAL     : Specify the target of the operation as either bucket or disk

  Example: ksanFSCK --target bucket --bucketName bucket1 
  Example: ksanFSCK --target bucket --bucketName bucket1 --checkOnly 
  Example: ksanFSCK --target disk --diskid disk111 
  Example: ksanFSCK --target disk --diskid disk111 --checkOnly 
```

## ksanGetattr
It is a utility to get and display the metadata of an object.

### Usage
```sh
 ksanGetattr  --help
 --bucketName VAL : Specify the name of the bucket
 --help           : To display this help menu
 --isBucket       : set it if you wish to display the attribute of a bucket
 --key VAL        : Specify the object path
 --objId VAL      : Specify the object Id if you with to display with Id rather
                    than object key
 --version VAL    : Specify the object version Id if you wish particula version
                    of the object

  Example: ksanGetattr --bucketName bucket1 --key file1.txt 
  Example: ksanGetattr --bucketName bucket1 --objid bd01856bfd2065d0d1ee20c03bd3a9af 

```

## ksanCbalance
It is a utility to move object or speficed amount of object from one OSD disk into others avliable disk.
### Usage
```sh
ksanCbalance  --help
 --DstDiskId VAL  : Specify the distination disk Id
 --SrcDiskId VAL  : Specify the source disk Id
 --bucketName VAL : Specify the name of the bucket you wish to balance
 --help           : To display this help menu
 --key VAL        : Specify the object path
 --objId VAL      : Specify the object Id insted of object key
 --size VAL       : Specify the capacity to move
 --target VAL     : Specify the target of the operation as moving a single
                    object or specfic amount of objects move

Example : To move a single object and the object can be idetified either key or object Id
          ksanCbalance --target object --bucketName bucket1 --key file1.txt 
          ksanCbalance --target bucket --bucketName bucket1 --objId bd01856bfd2065d0d1ee20c03bd3a9af 
          ksanCbalance --target object --bucketName bucket1 --key file1.txt --DstDiskId disk222

Example : To move a spefic amount of object from one disk to others
         ksanCbalance --target size --SrcDiskId disk111  --DstDiskId disk222 --size 2GB 
         ksanCbalance --target size --SrcDiskId disk111 --size 2GB 
```
## ksanRecovery
It a demonized program that will recover the object replica in case of failure.  
### Usage
```sh
ksanRecovery  --daemon 
```

