#!/bin/bash
basepath=$(cd `dirname $0`; pwd)
cd "$basepath"
pwd
echo "--------------------        Start Packaging        --------------------"
echo "1: Ryoyuhk"
echo "2: Dockyard"
echo "3: Bindo Alpha"
echo "4: TasteGourmet"
read -p "Pls select project you want package:" num
if [ "$num" -eq "1" ];then
./gradlew assembleryoyuhkRelease
echo "♡ Package Finish ♡"
elif [ "$num" -eq "2" ];then
./gradlew assembledockyardRelease
echo "♡ Package Finish ♡"
elif [ "$num" -eq "3" ];then
./gradlew assemblebindoalphaRelease
echo "♡ Package Finish ♡"
elif [ "$num" -eq "4" ];then
./gradlew assembletastegourmetRelease
echo "♡ Package Finish ♡"
else
echo "Invalid input, pls try again~!!!"
fi