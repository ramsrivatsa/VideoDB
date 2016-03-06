# when adding more machines, add ssh free login
MACHINES=( 'clarity25' 'clarity26'  )

GC_LOG_DIR=/home/ramsri/storm_gc_log
STORM_LOG_DIR=/usr/local/storm/logs
STORMCV_DATA_DIR=/home/ramsri/stormcv_data

collect_data()
{
  folder_name=run-`date +%s`
  mkdir $STORMCV_DATA_DIR/$folder_name
  for l in ${MACHINES[*]}
  do
    mkdir $STORMCV_DATA_DIR/$folder_name/$l
    mkdir $STORMCV_DATA_DIR/$folder_name/$l/GC
    mkdir $STORMCV_DATA_DIR/$folder_name/$l/logs
    scp -r ramsri@$l.eecs.umich.edu:$GC_LOG_DIR/* $STORMCV_DATA_DIR/$folder_name/$l/GC/
    scp -r ramsri@$l.eecs.umich.edu:$STORM_LOG_DIR/* $STORMCV_DATA_DIR/$folder_name/$l/logs/
  done

}


collect_data
