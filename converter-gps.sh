
cat miejscowosci2.csv| awk -F, 'BEGIN{OFS=","} {print $1, $2, substr($3,2,2)+substr($3,5,2)/60 + substr($3,8,2)/3600, substr($3,13,2) + substr($3,16,2)/60 + substr($3,19,2)/3600}' >| miejscowosci3.csv

