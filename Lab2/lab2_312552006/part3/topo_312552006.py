from mininet.topo import Topo

class Lab2_Topo_312552006( Topo ):
    def __init__( self ):
        Topo.__init__( self )

        hosts, switches = {}, {}
        for i in range(1,4):
            host_name = f'h{i}'
            switch_name = f's{i}'

            hosts[host_name] = self.addHost(host_name)
            switches[switch_name] = self.addSwitch(switch_name)

            self.addLink( hosts[host_name], switches[switch_name])
        
        self.addLink( switches['s1'], switches['s2'] )
        self.addLink( switches['s2'], switches['s3'] )
        self.addLink( switches['s1'], switches['s3'] )

topos = { 'topo_312552006': Lab2_Topo_312552006 }