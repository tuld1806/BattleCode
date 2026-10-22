#include <iostream>
#include <string>

// Keep this process alive for the whole match. Replace the empty action list
// with a JSON action for each friendly unit you want to command.
int main() {
    std::cout << "READY" << std::endl;
    std::string observation;
    while (std::getline(std::cin, observation)) {
        std::cout << "{\"actions\":[]}" << std::endl;
    }
    return 0;
}
