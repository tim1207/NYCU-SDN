# Start ONOS in localhost
cd $ONOS_ROOT
bazel run onos-local -- clean debug
ok clean 

# Bring up another new terminal and enter ONOS CLI
cd $ONOS_ROOT
tools/test/bin/onos localhost

# onos
apps -a -s
app activate <name>
app activate fwd # activate ReactiveForwarding
app deactivate <name>

# mininet

sudo mn --controller=remote,127.0.0.1:6653 --switch=ovs,protocols=OpenFlow14
sudo mn --topo=linear,3 --controller=remote,127.0.0.1:6653 --switch=ovs,protocols=OpenFlow14
sudo mn --custom=lab1_part2_<studentID>.py --topo=topo_part2_<studentID> --controller=remote,ip=127.0.0.1:6653 --switch=ovs,protocols=OpenFlow14

sudo mn -c

# Curl ─ Command Tool for Transferring Data with URL
curl -u <user:password> -X <method-type> -H <header> -d @<filename> [URL...]

curl -u onos:rocks -X POST -H 'Content-Type: application/json' -d @flows_s1-1_312552006.json 'http://localhost:8181/onos/v1/flows/of:0000000000000001'

curl -u onos:rocks -X DELETE -H 'Accept: application/json' 'http://localhost:8181/onos/v1/flows/of:0000000000000001/49539598402319842'

curl -u onos:rocks -X GET -H 'Accept: application/json' \
'http://localhost:8181/onos/v1/flows/of:0000000000000001'

# of:0000000000000001 => deviceID 
# 49539596291667367 => flow ID