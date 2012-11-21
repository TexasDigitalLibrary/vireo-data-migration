
#/bin/bash

CLASSPATH=$(echo lib/*.jar | tr ' ' ':')

echo "Starting vireo migration: Settings, People, Submissions, Items, Logs, and Committee"

echo ""

echo "[Step: 0/6] Starting settings migration"
groovy -cp $CLASSPATH settings.groovy
if [[ $? -ne 0 ]]; then exit 1; fi
echo "[Step: 1/6] Settings migration completed"

echo ""

echo "[Step: 1/6] Starting people migration"
groovy -cp $CLASSPATH person.groovy
if [[ $? -ne 0 ]]; then exit 1; fi
echo "[Step: 2/6] People migration completed"

echo ""

exit

echo "[Step: 2/6] Starting submission migration"
groovy -cp $CLASSPATH submission.groovy
if [[ $? -ne 0 ]]; then exit 1; fi
echo "[Step: 3/6] Submission migration completed"

echo ""

echo "[Step: 3/6] Starting item migration"
groovy -cp $CLASSPATH item.groovy
if [[ $? -ne 0 ]]; then exit 1; fi
echo "[Step: 4/6] Item migration completed"

echo ""

echo "[Step: 4/6] Starting log migration"
groovy -cp $CLASSPATH log.groovy
if [[ $? -ne 0 ]]; then exit 1; fi
echo "[Step: 5/6] Log migration completed"

echo ""

echo "[Step: 5/6] Starting committee migration"
groovy -cp $CLASSPATH committee.groovy
if [[ $? -ne 0 ]]; then exit 1; fi
echo "[Step: 6/6] Committee migration complete"

echo ""

echo "Vireo migration complete"
