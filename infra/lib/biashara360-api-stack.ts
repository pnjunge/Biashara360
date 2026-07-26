import * as cdk from 'aws-cdk-lib'
import * as acm from 'aws-cdk-lib/aws-certificatemanager'
import * as cloudwatch from 'aws-cdk-lib/aws-cloudwatch'
import * as actions from 'aws-cdk-lib/aws-cloudwatch-actions'
import * as ec2 from 'aws-cdk-lib/aws-ec2'
import * as ecr from 'aws-cdk-lib/aws-ecr'
import * as ecs from 'aws-cdk-lib/aws-ecs'
import * as elbv2 from 'aws-cdk-lib/aws-elasticloadbalancingv2'
import * as events from 'aws-cdk-lib/aws-events'
import * as targets from 'aws-cdk-lib/aws-events-targets'
import * as logs from 'aws-cdk-lib/aws-logs'
import * as route53 from 'aws-cdk-lib/aws-route53'
import * as route53Targets from 'aws-cdk-lib/aws-route53-targets'
import * as secretsmanager from 'aws-cdk-lib/aws-secretsmanager'
import * as sns from 'aws-cdk-lib/aws-sns'
import * as subscriptions from 'aws-cdk-lib/aws-sns-subscriptions'
import { Construct } from 'constructs'

export class Biashara360ApiStack extends cdk.Stack {
  constructor(scope: Construct, id: string, props: cdk.StackProps) {
    super(scope, id, props)

    const context = (key: string): string => {
      const value = this.node.tryGetContext(key)
      if (!value) throw new Error(`Missing CDK context value: ${key}`)
      return value
    }

    const vpc = ec2.Vpc.fromLookup(this, 'Vpc', { vpcId: context('vpcId') })
    const hostedZone = route53.HostedZone.fromLookup(this, 'HostedZone', {
      domainName: context('hostedZoneName')
    })
    const apiDomainName = context('apiDomainName')
    const certificate = new acm.Certificate(this, 'ApiCertificate', {
      domainName: apiDomainName,
      validation: acm.CertificateValidation.fromDns(hostedZone)
    })

    const alertTopic = new sns.Topic(this, 'OperationsAlerts', {
      displayName: 'Biashara360 production alerts'
    })
    const alertEmail = this.node.tryGetContext('alertEmail')
    if (alertEmail) alertTopic.addSubscription(new subscriptions.EmailSubscription(alertEmail))

    const cluster = new ecs.Cluster(this, 'Cluster', {
      vpc,
      containerInsightsV2: ecs.ContainerInsights.ENHANCED
    })
    const apiLogGroup = new logs.LogGroup(this, 'ApiLogs', {
      logGroupName: '/biashara360/production/api',
      retention: logs.RetentionDays.ONE_MONTH,
      removalPolicy: cdk.RemovalPolicy.RETAIN
    })

    const taskDefinition = new ecs.FargateTaskDefinition(this, 'ApiTask', {
      cpu: 512,
      memoryLimitMiB: 1024,
      runtimePlatform: {
        cpuArchitecture: ecs.CpuArchitecture.X86_64,
        operatingSystemFamily: ecs.OperatingSystemFamily.LINUX
      }
    })
    const repository = ecr.Repository.fromRepositoryName(this, 'ApiRepository', 'biashara360-api')
    const apiSecret = secretsmanager.Secret.fromSecretNameV2(
      this,
      'ApiRuntimeSecret',
      context('apiSecretName')
    )
    const container = taskDefinition.addContainer('api', {
      image: ecs.ContainerImage.fromEcrRepository(repository, context('apiImageTag')),
      environment: {
        API_PORT: '8080',
        PORT: '8080',
        DB_USER: 'postgres',
        DATABASE_URL: 'jdbc:postgresql://biashara360-db.ckrko2ecqjqy.us-east-1.rds.amazonaws.com:5432/biashara360',
        MPESA_CALLBACK_URL: `https://${apiDomainName}/v1/payments/mpesa/callback`
      },
      secrets: {
        DB_PASSWORD: ecs.Secret.fromSecretsManager(apiSecret, 'DB_PASSWORD'),
        JWT_SECRET: ecs.Secret.fromSecretsManager(apiSecret, 'JWT_SECRET'),
        MPESA_CONSUMER_KEY: ecs.Secret.fromSecretsManager(apiSecret, 'MPESA_CONSUMER_KEY'),
        MPESA_CONSUMER_SECRET: ecs.Secret.fromSecretsManager(apiSecret, 'MPESA_CONSUMER_SECRET'),
        SUPERUSER_PASSWORD: ecs.Secret.fromSecretsManager(apiSecret, 'SUPERUSER_PASSWORD')
      },
      healthCheck: {
        command: ['CMD-SHELL', 'wget -q -O /dev/null http://localhost:8080/v1/health || exit 1'],
        interval: cdk.Duration.seconds(30),
        timeout: cdk.Duration.seconds(5),
        retries: 3,
        startPeriod: cdk.Duration.seconds(120)
      },
      logging: ecs.LogDrivers.awsLogs({
        logGroup: apiLogGroup,
        streamPrefix: 'api',
        mode: ecs.AwsLogDriverMode.BLOCKING
      })
    })
    container.addPortMappings({ containerPort: 8080, protocol: ecs.Protocol.TCP })

    const serviceSecurityGroup = new ec2.SecurityGroup(this, 'ServiceSecurityGroup', {
      vpc,
      allowAllOutbound: true,
      description: 'Biashara360 API tasks; inbound only from the ALB'
    })
    const loadBalancerSecurityGroup = new ec2.SecurityGroup(this, 'LoadBalancerSecurityGroup', {
      vpc,
      allowAllOutbound: true,
      description: 'Public HTTPS access to Biashara360 API'
    })
    loadBalancerSecurityGroup.addIngressRule(ec2.Peer.anyIpv4(), ec2.Port.tcp(443), 'Public HTTPS')
    serviceSecurityGroup.addIngressRule(loadBalancerSecurityGroup, ec2.Port.tcp(8080), 'ALB to API')

    const databaseSecurityGroup = ec2.SecurityGroup.fromSecurityGroupId(
      this,
      'DatabaseSecurityGroup',
      context('databaseSecurityGroupId'),
      { mutable: true }
    )
    databaseSecurityGroup.addIngressRule(serviceSecurityGroup, ec2.Port.tcp(5432), 'API tasks to PostgreSQL')

    const service = new ecs.FargateService(this, 'ApiService', {
      cluster,
      taskDefinition,
      desiredCount: 2,
      assignPublicIp: true,
      securityGroups: [serviceSecurityGroup],
      vpcSubnets: { subnetType: ec2.SubnetType.PUBLIC },
      healthCheckGracePeriod: cdk.Duration.seconds(120),
      circuitBreaker: { rollback: true },
      minHealthyPercent: 100,
      maxHealthyPercent: 200,
      enableExecuteCommand: false
    })

    const loadBalancer = new elbv2.ApplicationLoadBalancer(this, 'ApiLoadBalancer', {
      vpc,
      internetFacing: true,
      securityGroup: loadBalancerSecurityGroup,
      vpcSubnets: { subnetType: ec2.SubnetType.PUBLIC }
    })
    loadBalancer.addRedirect({ sourcePort: 80, targetPort: 443, targetProtocol: elbv2.ApplicationProtocol.HTTPS })
    const listener = loadBalancer.addListener('HttpsListener', {
      port: 443,
      protocol: elbv2.ApplicationProtocol.HTTPS,
      certificates: [certificate],
      sslPolicy: elbv2.SslPolicy.RECOMMENDED_TLS
    })
    const targetGroup = listener.addTargets('ApiTargets', {
      port: 8080,
      protocol: elbv2.ApplicationProtocol.HTTP,
      targets: [service],
      healthCheck: {
        path: '/v1/health',
        healthyHttpCodes: '200',
        interval: cdk.Duration.seconds(30),
        timeout: cdk.Duration.seconds(5),
        healthyThresholdCount: 2,
        unhealthyThresholdCount: 2
      },
      deregistrationDelay: cdk.Duration.seconds(30)
    })
    new route53.ARecord(this, 'ApiAlias', {
      zone: hostedZone,
      recordName: 'api',
      target: route53.RecordTarget.fromAlias(new route53Targets.LoadBalancerTarget(loadBalancer))
    })

    const alarmDefaults = {
      evaluationPeriods: 3,
      datapointsToAlarm: 2,
      treatMissingData: cloudwatch.TreatMissingData.NOT_BREACHING
    }
    const api5xxAlarm = new cloudwatch.Alarm(this, 'Api5xxAlarm', {
      metric: new cloudwatch.MathExpression({
        expression: 'IF(requests > 0, errors * 100 / requests, 0)',
        usingMetrics: {
          errors: loadBalancer.metrics.httpCodeElb(elbv2.HttpCodeElb.ELB_5XX_COUNT, {
            period: cdk.Duration.minutes(1),
            statistic: 'Sum'
          }),
          requests: loadBalancer.metrics.requestCount({
            period: cdk.Duration.minutes(1),
            statistic: 'Sum'
          })
        },
        label: 'API 5xx rate (%)',
        period: cdk.Duration.minutes(1)
      }),
      threshold: 5,
      comparisonOperator: cloudwatch.ComparisonOperator.GREATER_THAN_THRESHOLD,
      ...alarmDefaults
    })

    const paymentFailureFilter = apiLogGroup.addMetricFilter('PaymentFailureMetric', {
      filterPattern: logs.FilterPattern.literal('{ $.event = "payment_failure" }'),
      metricNamespace: 'Biashara360/Production',
      metricName: 'PaymentFailures',
      metricValue: '1',
      defaultValue: 0
    })
    const paymentFailureAlarm = new cloudwatch.Alarm(this, 'PaymentFailureAlarm', {
      metric: paymentFailureFilter.metric({ period: cdk.Duration.minutes(1), statistic: 'Sum' }),
      threshold: 3,
      comparisonOperator: cloudwatch.ComparisonOperator.GREATER_THAN_OR_EQUAL_TO_THRESHOLD,
      ...alarmDefaults
    })
    const databaseFailureFilter = apiLogGroup.addMetricFilter('DatabaseFailureMetric', {
      filterPattern: logs.FilterPattern.literal('{ $.event = "database_connection_failure" }'),
      metricNamespace: 'Biashara360/Production',
      metricName: 'DatabaseConnectionFailures',
      metricValue: '1',
      defaultValue: 0
    })
    const databaseFailureAlarm = new cloudwatch.Alarm(this, 'DatabaseFailureAlarm', {
      metric: databaseFailureFilter.metric({ period: cdk.Duration.minutes(1), statistic: 'Sum' }),
      threshold: 1,
      comparisonOperator: cloudwatch.ComparisonOperator.GREATER_THAN_OR_EQUAL_TO_THRESHOLD,
      evaluationPeriods: 1,
      datapointsToAlarm: 1,
      treatMissingData: cloudwatch.TreatMissingData.NOT_BREACHING
    })
    ;[api5xxAlarm, paymentFailureAlarm, databaseFailureAlarm].forEach(alarm => {
      alarm.addAlarmAction(new actions.SnsAction(alertTopic))
      alarm.addOkAction(new actions.SnsAction(alertTopic))
    })

    new events.Rule(this, 'DeploymentFailureNotification', {
      eventPattern: {
        source: ['aws.ecs'],
        detailType: ['ECS Deployment State Change'],
        detail: {
          eventType: ['ERROR'],
          eventName: ['SERVICE_DEPLOYMENT_FAILED']
        }
      },
      targets: [new targets.SnsTopic(alertTopic)]
    })

    const dashboard = new cloudwatch.Dashboard(this, 'OperationsDashboard', {
      dashboardName: 'Biashara360-Production'
    })
    dashboard.addWidgets(
      new cloudwatch.GraphWidget({
        title: 'API requests and 5xx',
        left: [loadBalancer.metrics.requestCount()],
        right: [loadBalancer.metrics.httpCodeElb(elbv2.HttpCodeElb.ELB_5XX_COUNT)]
      }),
      new cloudwatch.GraphWidget({
        title: 'Payment and database failures',
        left: [paymentFailureFilter.metric(), databaseFailureFilter.metric()]
      })
    )

    new cdk.CfnOutput(this, 'ApiUrl', { value: `https://${apiDomainName}` })
    new cdk.CfnOutput(this, 'ClusterName', { value: cluster.clusterName })
    new cdk.CfnOutput(this, 'ServiceName', { value: service.serviceName })
    new cdk.CfnOutput(this, 'TargetGroupArn', { value: targetGroup.targetGroupArn })
    new cdk.CfnOutput(this, 'AlertsTopicArn', { value: alertTopic.topicArn })
  }
}
