LIST="$1"

for name in $LIST
do
  echo "#! /bin/bash" >"build/jar/$name"
  echo "" >> "build/jar/$name"
  echo "parent_path=\$( cd \"\$(dirname \"\${BASH_SOURCE[0]}\")\" ; pwd -P )" >> "build/jar/$name"
  echo "java -jar \$parent_path/$name.jar \"\$@\" " >> "build/jar/$name"
  chmod +x "build/jar/$name"
  echo "build/jar/$name"
done
