public class Helper {
    /*
     calculate UCB:
        Q(s,a) = value of node / number of time node has been visited [notice this is the same as MAB]
        Constant to handle exploitation importance
        Exploration term = sqrt(log(number of times parent node visited) / number of time current node visited)
    */
    public double UCB(int childSumValue, int childNumberOfVisits, int parentNumberOfVisits, int constant){
        double q_value = (double) childSumValue / childNumberOfVisits;
        double exploitationTerm = (double) constant * Math.sqrt(Math.log(parentNumberOfVisits) / childNumberOfVisits);
        return exploitationTerm - q_value;
    }

     /*
     calculate PUCB:
        Q(s,a) = value of node / number of time node has been visited [notice this is the same as MAB]
        Constant PUCB: affect importance of P(s,a)
        P(s'|s,a) = probability of transitioning to state s'
        Exploration term = c_pucb * p(s'|s,a) * log(number of times parent node visited / number of time current node visited)
    */

    public double PUCB(int childSumValue, int childNumberOfVisits, int parentNumberOfVisits, int constant_pucb, int probability_transition){
        double q_value = (double) childSumValue / childNumberOfVisits;
        double exploitationTerm = (double) constant_pucb * probability_transition * (Math.log(parentNumberOfVisits) / childNumberOfVisits);
        return exploitationTerm - q_value;
    }

}
