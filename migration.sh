
#/bin/bash

CLASSPATH=$(echo lib/*.jar | tr ' ' ':')

echo "Starting vireo migration: People, Submissions, Items, Logs, and Committee"

echo ""

echo "[Step: 0/5] Starting people migration"
groovy -cp $CLASSPATH person.groovy
echo "[Step: 1/5] People migration completed"

echo ""

echo "[Step: 1/5] Starting submission migration"
groovy -cp $CLASSPATH submission.groovy
echo "[Step: 2/5] Submission migration completed"

echo ""

echo "[Step: 2/5] Starting item migration"
groovy -cp $CLASSPATH item.groovy
echo "[Step: 3/5] Item migration completed"

echo ""

echo "[Step: 3/5] Starting log migration"
groovy -cp $CLASSPATH log.groovy
echo "[Step: 4/5] Log migration completed"

echo ""

echo "[Step: 4/5] Starting committee migration"
groovy -cp $CLASSPATH committee.groovy
echo "[Step: 5/5] Committee migration complete"

echo ""

echo "Vireo migration complete"
