#!/bin/sh

start_week=1
end_week=8
members=("종민" "보배" "성민" "재민" "예원" "성찬")

week=$start_week
while [ $week -le $end_week ]
do
  week_folder="Week-$week"
  mkdir -p "$week_folder"

  for member in ${members[@]}
  do
     mkdir -p "$week_folder/$member"
     touch "$week_folder/$member/dummy"
  done

  week=$((week+1))
done
