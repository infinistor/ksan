#! /bin/bash

PARENTPATH=$( cd "$(dirname "${BASH_SOURCE[0]}")" ; pwd -P )
make_excuteable(){
Fname="$1"

FULLPATH="$PARENTPATH/../target/$Fname"
echo "#! /bin/bash" >$FULLPATH
echo "" >> $FULLPATH
echo "parent_path=\$( cd \"\$(dirname \"\$\{BASH_SOURCE\[0\]\}\")\" ; pwd -P )" >> $FULLPATH
echo "[ ! -f "$parent_path/$Fname.jar" ] && parent_path="/usr/local/ksan/bin"" >> $FULLPATH
echo "[ ! -f "$parent_path/$Fname.jar" ] && parent_path="/usr/local/ksan/bin/util"" >> $FULLPATH
echo "java -jar -Dlogback.configurationFile=/usr/local/ksan/etc/ksanRecovery_log_conf.xml \$parent_path/$Fname.jar \"\$@\"" >> $FULLPATH
echo "" >> $FULLPATH

chmod +x "$FULLPATH"
}

#make_excuteable "ksanGetAttr"
make_excuteable "ksanRecovery"
#make_excuteable "ksanFsck"
#make_excuteable "ksanCbalance"

exit 0
