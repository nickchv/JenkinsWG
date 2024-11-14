pipeline {
    agent any
    
    parameters {
        string(name: 'CONFIG_NAME', defaultValue: '', description: 'Enter the configuration name')
        string(name: 'IP_NUMBER', defaultValue: '', description: 'Enter the IP address')
        
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }
        
        stage ('Generate Public and Private Keys'){
            steps {
                script {
                    def keys_gen = sh(returnStatus: true, script:' wg genkey | tee /etc/wireguard/${CONFIG_NAME}_privatekey | wg pubkey | tee /etc/wireguard/${CONFIG_NAME}_publickey')
                   
                }
            }
        }

        stage('Edit config') {
            steps {
                script {
                    def public_key = sh(returnStdout: true, script: 'cat /etc/wireguard/${CONFIG_NAME}_publickey').trim()
                    def result = sh(returnStatus: true, script: """
                        {
                            echo ""
                            echo "#${CONFIG_NAME}"
                            echo "[Peer]"
                            echo "PublicKey = ${public_key}"
                            echo "AllowedIPs = ${IP_NUMBER}/32"
                        } >> "/etc/wireguard/wg0.conf"
                    """)

                    if (result == 0) {
                        println("Строки успешно добавлены в /etc/wireguard/wg0.conf.")
                    } else {
                        error('ERROR')
                    }
                }
            }
        }

        stage('Generate Config') {
            steps {
                script {
                    def private_key = sh(returnStdout: true, script: 'cat /etc/wireguard/${CONFIG_NAME}_privatekey').trim()
                    def public_key_server = sh(returnStdout: true, script: 'cat /etc/wireguard/server_publickey').trim()
            
            // Генерация конфигурации
                    def config = """
                    [Interface]
                    PrivateKey = ${private_key}
                    Address = ${IP_NUMBER}/32
                    DNS = 8.8.8.8, 1.1.1.1

                    [Peer]
                    PublicKey = ${public_key_server}
                    AllowedIPs = 0.0.0.0/0, ::/0
                    Endpoint = 5.42.101.240:10886
                    PersistentKeepalive = 20
                    """
            
            // Вывод конфигурации
                    println(config)

            // Перезагрузка WireGuard
                    def restart_wg = sh(returnStdout: true, script: 'systemctl restart wg-quick@wg0.service').trim()
                    println(restart_wg)
                }
            }
        }
    }
    
    post {
        always {
            echo 'Pipeline finished.'
        }
    }
}
