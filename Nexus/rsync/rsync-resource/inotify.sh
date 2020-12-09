host=39.98.153.232
src=/home/nexus
des=rsync
password=/etc/rsyncd.secrets
user=root
inotifywait=/usr/local/bin/inotifywait

$inotifywait -mrq --timefmt '%Y%m%d %H:%M' --format '%T %w%f%e' -e modify,delete,create,attrib $src \
| while read files ; do
    rsync -avzP --delete  --timeout=100 --password-file=${password} $src $user@$host::$des
    echo "${files} was rsynced" >>/tmp/rsync.log 2>&1
done
