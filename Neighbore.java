public class Neighbore
{
    protected int main_router;
    protected int neighbore_router;
    protected String ip_router;
    protected int udp_port_neighbore;
    protected int tcp_port_neighbore;
    protected int edge_weight;

    public Neighbore(int main_router, int neighbore_router, String ip_router, int udp_port_neighbore,
                     int tcp_port_neighbore, int edge_weight)
    {
        this.main_router = main_router;
        this.neighbore_router = neighbore_router;
        this.ip_router = ip_router;
        this.udp_port_neighbore = udp_port_neighbore;
        this.tcp_port_neighbore = tcp_port_neighbore;
        this.edge_weight = edge_weight;
    }
}
